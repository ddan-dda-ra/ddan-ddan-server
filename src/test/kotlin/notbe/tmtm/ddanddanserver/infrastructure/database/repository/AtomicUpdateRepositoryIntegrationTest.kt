package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 원자적 갱신 쿼리 통합 테스트.
 *
 * 필드 이름 매핑(snake_case), 조건부 갱신, 동시 요청에서 값이 덮어써지지 않는지는 mock으로 검증할 수 없어 실제 MongoDB가 필요하다.
 * 실행 조건과 DB는 [UserStatRepositoryDateRangeIntegrationTest]와 같다.
 *  - `RUN_MONGO_IT=true`일 때만 실행한다.
 *  - 전용 throwaway DB(ddan-ddan-it-test)를 쓰고, 테스트 전후로 사용 컬렉션을 비운다.
 *
 * 실행: `RUN_MONGO_IT=true MONGO_IT_USER=... MONGO_IT_PASSWORD=... ./gradlew test --tests "*AtomicUpdateRepositoryIntegrationTest"`
 */
@DataMongoTest
@TestPropertySource(
    properties = [
        "spring.data.mongodb.host=\${MONGO_IT_HOST:localhost}",
        "spring.data.mongodb.port=\${MONGO_IT_PORT:27017}",
        "spring.data.mongodb.database=ddan-ddan-it-test",
        "spring.data.mongodb.username=\${MONGO_IT_USER:}",
        "spring.data.mongodb.password=\${MONGO_IT_PASSWORD:}",
        "spring.data.mongodb.authentication-database=admin",
        "spring.data.mongodb.field-naming-strategy=org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy",
        "spring.data.mongodb.auto-index-creation=true",
        "de.flapdoodle.mongodb.embedded.version=",
    ],
)
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_IT", matches = "true")
class AtomicUpdateRepositoryIntegrationTest {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var petRepository: PetRepository

    @Autowired
    lateinit var dailyInfoRepository: DailyInfoRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        listOf(User::class.java, Pet::class.java, DailyInfo::class.java).forEach {
            mongoTemplate.remove(org.springframework.data.mongodb.core.query.Query(), it)
        }
    }

    @Test
    fun `동시에 먹이를 소모하면 가진 수량만큼만 성공해야 한다`() {
        val user = mongoTemplate.insert(newUser(foodQuantity = 3))

        val results = runConcurrently(10) {
            runCatching { userRepository.consumeFood(user.id, 1) }
        }

        assertEquals(3, results.count { it.isSuccess })
        assertTrue(results.filter { it.isFailure }.all { it.exceptionOrNull() is UserFoodQuantityLackException })
        assertEquals(0, rawUser(user.id).getInteger("food_quantity"))
    }

    @Test
    fun `동시에 칼로리를 올리면 각 요청이 받는 이전 칼로리가 겹치지 않아 보상이 한 번만 계산돼야 한다`() {
        val dailyInfo = mongoTemplate.insert(DailyInfo.create(ObjectId(), "유저", "DOG", LocalDate.now()))

        val previousCalories = runConcurrently(10) {
            dailyInfoRepository.raiseCalorie(dailyInfo.id, 250)
        }

        // 한 요청만 0을 받고 나머지는 이미 올라간 250을 받는다.
        assertEquals(1, previousCalories.count { it == 0 })
        assertEquals(9, previousCalories.count { it == 250 })
        assertEquals(250, rawDailyInfo(dailyInfo.id).getInteger("calorie"))
    }

    @Test
    fun `칼로리는 낮은 값으로 내려가지 않아야 한다`() {
        val dailyInfo = mongoTemplate.insert(DailyInfo.create(ObjectId(), "유저", "DOG", LocalDate.now(), calorie = 300))

        assertEquals(300, dailyInfoRepository.raiseCalorie(dailyInfo.id, 100))
        assertEquals(300, rawDailyInfo(dailyInfo.id).getInteger("calorie"))
    }

    @Test
    fun `칼로리 되돌리기는 그 사이 다른 요청이 더 올렸으면 건드리지 않아야 한다`() {
        val dailyInfo = mongoTemplate.insert(DailyInfo.create(ObjectId(), "유저", "DOG", LocalDate.now(), calorie = 500))

        dailyInfoRepository.revertCalorie(dailyInfo.id, from = 300, to = 100)
        assertEquals(500, rawDailyInfo(dailyInfo.id).getInteger("calorie"))

        dailyInfoRepository.revertCalorie(dailyInfo.id, from = 500, to = 100)
        assertEquals(100, rawDailyInfo(dailyInfo.id).getInteger("calorie"))
    }

    @Test
    fun `동시에 목표 달성 처리를 해도 한 요청만 성공해야 한다`() {
        val dailyInfo = mongoTemplate.insert(DailyInfo.create(ObjectId(), "유저", "DOG", LocalDate.now()))

        val results = runConcurrently(10) { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) }

        assertEquals(1, results.count { it })
        assertTrue(rawDailyInfo(dailyInfo.id).getBoolean("purpose_achieved"))
    }

    @Test
    fun `동시에 아이템을 지급하면 모두 누적돼야 한다`() {
        val user = mongoTemplate.insert(newUser())

        runConcurrently(10) { userRepository.increaseItems(user.id, food = 1, toy = 2, tickets = 3) }

        val raw = rawUser(user.id)
        assertEquals(10, raw.getInteger("food_quantity"))
        assertEquals(20, raw.getInteger("toy_quantity"))
        assertEquals(30, raw.getInteger("tickets"))
    }

    @Test
    fun `칼로리 보상은 먹이, 연속 달성, 접속일만 바꾸고 다른 필드는 그대로 둬야 한다`() {
        val user = mongoTemplate.insert(newUser(foodQuantity = 1, toyQuantity = 7, purposeStrict = 2))
        val today = LocalDate.now()

        val rewarded = userRepository.rewardDailyCalorie(user.id, food = 2, purposeAchieved = true, lastLoginAt = today)

        assertEquals(3, rewarded.foodQuantity)
        assertEquals(3, rewarded.purposeStrict)
        assertEquals(7, rewarded.toyQuantity)
        assertEquals(today, rewarded.lastLoginAt)
        assertEquals("유저", rewarded.name)
    }

    @Test
    fun `필드 단위 갱신은 동시에 들어온 아이템 변경을 덮어쓰지 않아야 한다`() {
        val user = mongoTemplate.insert(newUser(foodQuantity = 5))
        val petId = ObjectId()

        // 설정·프로필·메인펫·디바이스 토큰을 바꾸는 동안 먹이가 계속 소모된다.
        val consumed = runConcurrently(8) { index ->
            when (index % 4) {
                0 -> userRepository.updateSetting(user.id, UserSetting(isAppPushOn = true))
                1 -> userRepository.updateProfile(user.id, "새이름", 500)
                2 -> userRepository.updateMainPetId(user.id, petId)
                else -> userRepository.updateDeviceToken(user.id, DeviceToken("new-token"))
            }
            runCatching { userRepository.consumeFood(user.id, 1) }
        }

        val saved = userRepository.findByIdOrThrow(user.id)
        assertEquals(5, consumed.count { it.isSuccess })
        assertEquals(0, saved.foodQuantity)
        assertEquals(true, saved.setting.isAppPushOn)
        assertEquals("새이름", saved.name)
        assertEquals(500, saved.purposeCalorie)
        assertEquals(petId, saved.mainPetId)
        assertEquals("new-token", saved.deviceToken?.value)
        assertEquals("new-token", rawUser(user.id).getString("device_token"))
    }

    @Test
    fun `연속 달성 초기화는 제외 대상과 이미 0인 유저를 건드리지 않아야 한다`() {
        val achieved = mongoTemplate.insert(newUser(purposeStrict = 4))
        val failed = mongoTemplate.insert(newUser(purposeStrict = 2, foodQuantity = 9))
        mongoTemplate.insert(newUser(purposeStrict = 0))

        val modified = userRepository.resetPurposeStrictExcept(listOf(achieved.id))

        assertEquals(1L, modified)
        assertEquals(4, rawUser(achieved.id).getInteger("purpose_strict"))
        assertEquals(0, rawUser(failed.id).getInteger("purpose_strict"))
        assertEquals(9, rawUser(failed.id).getInteger("food_quantity"))
    }

    @Test
    fun `동시에 경험치를 올리면 모두 누적돼야 한다`() {
        val pet = mongoTemplate.insert(Pet.register("DOG", ObjectId()))

        runConcurrently(10) { petRepository.increaseExp(pet.id, Pet.EXP_PER_FOOD) }

        assertEquals(1000, mongoTemplate.findById<Pet>(pet.id)!!.exp)
    }

    @Test
    fun `일일 정보의 이름과 펫 타입만 갱신하고 칼로리는 그대로 둬야 한다`() {
        val userId = ObjectId()
        val today = LocalDate.now()
        val dailyInfo = mongoTemplate.insert(DailyInfo.create(userId, "옛이름", "DOG", today, calorie = 400))

        dailyInfoRepository.updateUserName(userId, today, "새이름")
        dailyInfoRepository.updatePetType(userId, today, "CAT")

        val raw = rawDailyInfo(dailyInfo.id)
        assertEquals("새이름", raw.getString("user_name"))
        assertEquals("CAT", raw.getString("pet_type"))
        assertEquals(400, raw.getInteger("calorie"))
        assertFalse(raw.getBoolean("toy_given"))
    }

    private fun newUser(
        foodQuantity: Int = 0,
        toyQuantity: Int = 0,
        purposeStrict: Int = 0,
    ) = User(
        id = ObjectId(),
        deviceToken = DeviceToken("token"),
        name = "유저",
        foodQuantity = foodQuantity,
        toyQuantity = toyQuantity,
        purposeStrict = purposeStrict,
    )

    private fun rawUser(id: ObjectId): Document =
        mongoTemplate.getCollection("users").find(Document("_id", id)).first()!!

    private fun rawDailyInfo(id: ObjectId): Document =
        mongoTemplate.getCollection("daily_calories").find(Document("_id", id)).first()!!

    private fun <T> runConcurrently(count: Int, task: (Int) -> T): List<T> {
        val executor = Executors.newFixedThreadPool(count)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until count).map { index ->
                executor.submit<T> {
                    start.await()
                    task(index)
                }
            }
            start.countDown()
            return futures.map { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }
}
