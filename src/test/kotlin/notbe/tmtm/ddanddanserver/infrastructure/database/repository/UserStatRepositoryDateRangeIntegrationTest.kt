package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * UserStatRepositoryImpl aggregation 통합 테스트.
 *
 * MEMORY(PR #277): aggregation `.count()`/정렬/limit-없는 조회는 단위 테스트로 검증 불가 — 실제 MongoDB 필요.
 * 이 프로젝트는 embedded MongoDB(flapdoodle)/testcontainers 의존이 없으므로,
 * 로컬 docker `mongodb` 컨테이너에 연결한다.
 *
 * 안전장치:
 *  - `RUN_MONGO_IT=true` 환경변수가 있을 때만 실행(평소/CI에서는 skip).
 *  - 운영 DB(dev-ddan-ddan-db / ddan-ddan-db)를 오염시키지 않도록 전용 throwaway DB(ddan-ddan-it-test) 사용.
 *  - 각 테스트 전후로 사용 컬렉션을 비운다.
 *
 * 실행: `RUN_MONGO_IT=true MONGO_IT_USER=... MONGO_IT_PASSWORD=... ./gradlew test --tests "*UserStatRepositoryDateRangeIntegrationTest"`
 */
@DataMongoTest
@Import(UserStatRepositoryImpl::class)
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
        "de.flapdoodle.mongodb.embedded.version=", // embedded mongo 자동구성 비활성(의존 없음)
    ],
)
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_IT", matches = "true")
class UserStatRepositoryDateRangeIntegrationTest {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var repository: UserStatRepositoryImpl

    // 직전 달 1일 ~ 말일 (테스트 기간)
    private val monthStart: LocalDate = LocalDate.now().minusMonths(1).withDayOfMonth(1)
    private val monthEnd: LocalDate = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
    // 전전달(기간 매칭 검증용)
    private val prevPrevStart: LocalDate = LocalDate.now().minusMonths(2).withDayOfMonth(1)

    @BeforeEach
    fun clean() {
        mongoTemplate.dropCollection("daily_calories")
        mongoTemplate.dropCollection("users")
        mongoTemplate.dropCollection("pets")
    }

    @AfterEach
    fun teardown() {
        mongoTemplate.dropCollection("daily_calories")
        mongoTemplate.dropCollection("users")
        mongoTemplate.dropCollection("pets")
    }

    private fun seedUserWithPet(name: String): User {
        val userId = ObjectId()
        val pet = Pet(id = ObjectId(), type = "CAT", ownerUserId = userId, exp = 0)
        mongoTemplate.save(pet)
        val user = User(id = userId, deviceToken = null, name = name, mainPetId = pet.id)
        mongoTemplate.save(user)
        return user
    }

    /** 같은 유저가 서로 다른 day일 동안 daily_calories 도큐먼트를 만든다 (출석일 = 도큐먼트 수). */
    private fun seedAttendance(
        user: User,
        days: Int,
        purposeAchievedDays: Int,
        caloriePerDay: Int = 100,
    ) {
        repeat(days) { i ->
            val info = DailyInfo.create(
                userId = user.id,
                userName = user.name,
                petType = "CAT",
                date = monthStart.plusDays(i.toLong()),
                calorie = caloriePerDay,
            )
            info.purposeAchieved = i < purposeAchievedDays
            mongoTemplate.save(info)
        }
    }

    @Test
    fun `출석일 집계는 daily_calories 도큐먼트 수를 정확히 세고 목표달성 여부와 무관하다`() {
        val user = seedUserWithPet("출석유저")
        // 5일 출석, 그중 2일만 목표달성
        seedAttendance(user, days = 5, purposeAchievedDays = 2)

        val result = repository.findRankingByDateRange(
            RankingCriteria.TOTAL_ATTENDANCE_DAYS,
            monthStart,
            monthEnd,
            10,
        )

        assertEquals(1, result.size)
        assertEquals(5, result[0].totalAttendanceDays) // 출석일 = 도큐먼트 수
        assertEquals(2, result[0].totalSucceededDays) // 목표달성일은 true 건수만
    }

    @Test
    fun `출석일 기준 정렬이 내림차순으로 동작한다 (silent 정렬 무력화 회귀 차단)`() {
        // 출석일 3 / 5 / 1 순서로 투입 → 결과는 5,3,1 (DESC)이어야 한다.
        // 필드명 불일치로 정렬이 무력화되면 입력 순서(3,5,1)로 반환되어 이 테스트가 실패한다.
        val u3 = seedUserWithPet("출석3일")
        val u5 = seedUserWithPet("출석5일")
        val u1 = seedUserWithPet("출석1일")
        seedAttendance(u3, days = 3, purposeAchievedDays = 0)
        seedAttendance(u5, days = 5, purposeAchievedDays = 0)
        seedAttendance(u1, days = 1, purposeAchievedDays = 0)

        val result = repository.findRankingByDateRange(
            RankingCriteria.TOTAL_ATTENDANCE_DAYS,
            monthStart,
            monthEnd,
            10,
        )

        assertEquals(listOf(5, 3, 1), result.map { it.totalAttendanceDays })
        assertEquals("출석5일", result[0].user.name)
    }

    @Test
    fun `findAllRankingByDateRange는 limit 없이 전체 유저를 반환한다`() {
        // TOP_N(3)보다 많은 5명 투입 → 전원 반환
        (5 downTo 1).forEach { days ->
            val u = seedUserWithPet("user-$days")
            seedAttendance(u, days = days, purposeAchievedDays = 0)
        }

        val result = repository.findAllRankingByDateRange(
            RankingCriteria.TOTAL_ATTENDANCE_DAYS,
            monthStart,
            monthEnd,
        )

        assertEquals(5, result.size)
        // 정렬 순서도 DESC 유지
        assertEquals(listOf(5, 4, 3, 2, 1), result.map { it.totalAttendanceDays })
    }

    @Test
    fun `전전달 데이터는 지난달 기간 조회에 섞이지 않는다 (date 경계 매칭)`() {
        val user = seedUserWithPet("경계유저")
        // 지난달 2일 출석
        seedAttendance(user, days = 2, purposeAchievedDays = 0)
        // 전전달 1일 출석(섞이면 안 됨)
        val prevInfo = DailyInfo.create(
            userId = user.id,
            userName = user.name,
            petType = "CAT",
            date = prevPrevStart,
            calorie = 100,
        )
        mongoTemplate.save(prevInfo)

        val result = repository.findRankingByDateRange(
            RankingCriteria.TOTAL_ATTENDANCE_DAYS,
            monthStart,
            monthEnd,
            10,
        )

        assertEquals(1, result.size)
        assertEquals(2, result[0].totalAttendanceDays) // 전전달 1일은 제외
    }
}
