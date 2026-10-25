package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

/**
 * 회원 탈퇴 시 정리 쿼리 통합 테스트.
 *
 * 랭킹보드는 Pair 목록에 User 문서가 내장된 구조라 `$pull` 경로가 실제 저장 형태와 맞는지 mock으로 확인할 수 없다.
 * 실행 조건과 DB는 [UserStatRepositoryDateRangeIntegrationTest]와 같다(`RUN_MONGO_IT=true`, 전용 DB ddan-ddan-it-test).
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
class WithdrawCleanupRepositoryIntegrationTest {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var rankingBoardRepository: RankingBoardRepository

    @Autowired
    lateinit var cheerRepository: CheerRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        mongoTemplate.remove(Query(), RankingBoardEntity::class.java)
        mongoTemplate.remove(Query(), Cheer::class.java)
    }

    @Test
    fun `모든 랭킹보드에서 탈퇴한 유저 항목만 제거해야 한다`() {
        val withdrawn = user("탈퇴유저")
        val remaining = user("남은유저")
        rankingBoardRepository.save(RankingBoard.of(PeriodType.MONTHLY, listOf(stat(withdrawn), stat(remaining))))
        rankingBoardRepository.save(RankingBoard.of(PeriodType.WEEKLY, listOf(stat(remaining), stat(withdrawn))))

        rankingBoardRepository.removeUser(withdrawn.id)

        listOf(PeriodType.MONTHLY, PeriodType.WEEKLY).forEach { periodType ->
            val board = rankingBoardRepository.findByIdOrDefault(periodType)
            assertEquals(listOf(remaining.id), board.ranking.map { it.second.user.id }, "$periodType")
        }
    }

    @Test
    fun `탈퇴한 유저가 보내거나 받은 응원만 삭제해야 한다`() {
        val withdrawnId = ObjectId()
        val other1 = ObjectId()
        val other2 = ObjectId()
        cheerRepository.saveAll(
            listOf(
                Cheer.create(withdrawnId, other1),
                Cheer.create(other1, withdrawnId),
                Cheer.create(other1, other2),
            ),
        )

        cheerRepository.deleteAllByCheererIdOrCheereeId(withdrawnId, withdrawnId)

        val left = cheerRepository.findAll()
        assertEquals(1, left.size)
        assertEquals(other1, left[0].cheererId)
        assertEquals(other2, left[0].cheereeId)
    }

    private fun user(name: String) = User(id = ObjectId(), deviceToken = DeviceToken("token-$name"), name = name)

    private fun stat(user: User) = UserStat(
        user = user,
        mainPet = Pet.register("DOG", user.id),
        totalCalories = 100,
        totalSucceededDays = 1,
        totalAttendanceDays = 1,
    )
}
