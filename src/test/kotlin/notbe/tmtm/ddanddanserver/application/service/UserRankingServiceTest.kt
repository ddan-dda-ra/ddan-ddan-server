package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserRankingServiceTest {
    private lateinit var userStatRepository: UserStatRepository
    private lateinit var userRankingService: UserRankingService

    @BeforeEach
    fun setUp() {
        userStatRepository = mockk()
        userRankingService = UserRankingService(userStatRepository)
    }

    @Test
    fun `총 칼로리 기준으로 랭킹을 조회해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()

        val userStatEntities =
            listOf(
                createUserStatEntity(userId1, "사용자1", 1000, 10),
                createUserStatEntity(userId2, "사용자2", 800, 15),
                createUserStatEntity(userId3, "사용자3", 600, 20),
            )

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(userId2, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        verify { userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY) }

        assertNotNull(result)
        assertEquals(3, result.rankings.size)

        // 랭킹 순서 확인 (칼로리 높은 순)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId1, result.rankings[0].userStat.userId)
        assertEquals(1000, result.rankings[0].userStat.totalCalories)

        assertEquals(2, result.rankings[1].rank)
        assertEquals(userId2, result.rankings[1].userStat.userId)
        assertEquals(800, result.rankings[1].userStat.totalCalories)

        assertEquals(3, result.rankings[2].rank)
        assertEquals(userId3, result.rankings[2].userStat.userId)
        assertEquals(600, result.rankings[2].userStat.totalCalories)

        // 내 랭킹 확인
        assertEquals(2, result.myRanking.rank)
        assertEquals(userId2, result.myRanking.userStat.userId)
    }

    @Test
    fun `총 성공일 기준으로 랭킹을 조회해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()

        val userStatEntities =
            listOf(
                createUserStatEntity(userId1, "사용자1", 500, 25), // 가장 많은 성공일
                createUserStatEntity(userId2, "사용자2", 1000, 20),
                createUserStatEntity(userId3, "사용자3", 800, 15),
            )

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(userId3, RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY)

        // then
        verify { userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY) }

        assertNotNull(result)
        assertEquals(3, result.rankings.size)

        // 랭킹 순서 확인 (성공일 높은 순)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId1, result.rankings[0].userStat.userId)
        assertEquals(25, result.rankings[0].userStat.totalSucceededDays)

        assertEquals(2, result.rankings[1].rank)
        assertEquals(userId2, result.rankings[1].userStat.userId)
        assertEquals(20, result.rankings[1].userStat.totalSucceededDays)

        assertEquals(3, result.rankings[2].rank)
        assertEquals(userId3, result.rankings[2].userStat.userId)
        assertEquals(15, result.rankings[2].userStat.totalSucceededDays)

        // 내 랭킹 확인
        assertEquals(3, result.myRanking.rank)
        assertEquals(userId3, result.myRanking.userStat.userId)
    }

    @Test
    fun `일간 기간 타입을 처리해야 한다`() {
        // given
        val userId = ObjectId()
        val userStatEntities =
            listOf(
                createUserStatEntity(userId, "사용자1", 100, 1),
            )

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(userId, RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY)

        // then
        verify { userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY) }
        assertNotNull(result)
        assertEquals(1, result.rankings.size)
        assertEquals(1, result.myRanking.rank)
    }

    @Test
    fun `연간 기간 타입을 처리해야 한다`() {
        // given
        val userId = ObjectId()
        val userStatEntities =
            listOf(
                createUserStatEntity(userId, "사용자1", 10000, 365),
            )

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(userId, RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY)

        // then
        verify { userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY) }
        assertNotNull(result)
        assertEquals(1, result.rankings.size)
        assertEquals(365, result.myRanking.userStat.totalSucceededDays)
    }

    @Test
    fun `대용량 데이터셋을 올바르게 처리해야 한다`() {
        // given
        val targetUserId = ObjectId()
        val userStatEntities =
            (1..200).map { index ->
                val userId = if (index == 150) targetUserId else ObjectId()
                createUserStatEntity(userId, "사용자$index", 2000 - index, index)
            }

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(targetUserId, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        verify { userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY) }

        assertNotNull(result)
        assertEquals(200, result.rankings.size)

        // 내 랭킹 확인 (150번째 사용자는 150위여야 함)
        assertEquals(150, result.myRanking.rank)
        assertEquals(targetUserId, result.myRanking.userStat.userId)

        // 상위 랭킹 제한 기능 확인
        val top10 = result.getTopRankings(10)
        assertEquals(10, top10.size)
        assertEquals(1, top10[0].rank)
        assertEquals(10, top10[9].rank)
    }

    @Test
    fun `서비스를 통해 동점 랭킹을 처리해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()

        val userStatEntities =
            listOf(
                createUserStatEntity(userId1, "사용자1", 1000, 10),
                createUserStatEntity(userId2, "사용자2", 800, 15), // 사용자3과 동점
                createUserStatEntity(userId3, "사용자3", 800, 20), // 사용자2와 동점
            )

        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = userRankingService.getRanking(userId2, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(2, result.rankings[1].rank) // 사용자2는 2위
        assertEquals(2, result.rankings[2].rank) // 사용자3도 2위 (동점)

        assertEquals(2, result.myRanking.rank)
        assertEquals(userId2, result.myRanking.userStat.userId)
    }

    private fun createUserStatEntity(
        userId: ObjectId,
        userName: String,
        totalCalories: Int,
        totalSucceededDays: Int,
    ): UserStatEntity {
        val entity = mockk<UserStatEntity>()
        every { entity.toDomain() } returns
            UserStat(
                userId = userId,
                userName = userName,
                mainPetType = PetType.DOG,
                totalCalories = totalCalories,
                totalSucceededDays = totalSucceededDays,
            )
        return entity
    }
}
