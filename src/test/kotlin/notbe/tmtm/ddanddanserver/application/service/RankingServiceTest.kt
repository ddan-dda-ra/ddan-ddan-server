package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.RankingBoardRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RankingServiceTest {
    private lateinit var userStatRepository: UserStatRepository
    private lateinit var rankingBoardRepository: RankingBoardRepository
    private lateinit var rankingService: RankingService

    private val user = createUser("user")
    private val pet = createPet(user.id)
    private val user1 = createUser("user1")
    private val pet1 = createPet(user1.id)
    private val user2 = createUser("user2")
    private val pet2 = createPet(user2.id)
    private val user3 = createUser("user3")
    private val pet3 = createPet(user3.id)

    @BeforeEach
    fun setUp() {
        userStatRepository = mockk()
        rankingBoardRepository = mockk()
        rankingService = RankingService(userStatRepository)
    }

    @Test
    fun `총 칼로리 기준으로 랭킹을 조회해야 한다`() {
        // given
        val userStatEntities =
            listOf(
                createUserStatEntity(user1, pet1, 1000, 10),
                createUserStatEntity(user2, pet2, 800, 15),
                createUserStatEntity(user3, pet3, 600, 20),
            )

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(user2.id, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        verify { userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY) }

        assertNotNull(result)
        assertEquals(3, result.rankings.size)

        // 랭킹 순서 확인 (칼로리 높은 순)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(1000, result.rankings[0].userStat.totalCalories)

        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(800, result.rankings[1].userStat.totalCalories)

        assertEquals(3, result.rankings[2].rank)
        assertEquals(user3, result.rankings[2].userStat.user)
        assertEquals(600, result.rankings[2].userStat.totalCalories)

        // 내 랭킹 확인
        assertEquals(2, result.myRanking.rank)
        assertEquals(user2, result.myRanking.userStat.user)
    }

    @Test
    fun `총 성공일 기준으로 랭킹을 조회해야 한다`() {
        // given
        val userStatEntities =
            listOf(
                createUserStatEntity(user1, pet1, 500, 25), // 가장 많은 성공일
                createUserStatEntity(user2, pet2, 1000, 20),
                createUserStatEntity(user3, pet3, 800, 15),
            )

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(user3.id, RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY)

        // then
        verify { userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.MONTHLY) }

        assertNotNull(result)
        assertEquals(3, result.rankings.size)

        // 랭킹 순서 확인 (성공일 높은 순)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(25, result.rankings[0].userStat.totalSucceededDays)

        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(20, result.rankings[1].userStat.totalSucceededDays)

        assertEquals(3, result.rankings[2].rank)
        assertEquals(user3, result.rankings[2].userStat.user)
        assertEquals(15, result.rankings[2].userStat.totalSucceededDays)

        // 내 랭킹 확인
        assertEquals(3, result.myRanking.rank)
        assertEquals(user3, result.myRanking.userStat.user)
    }

    @Test
    fun `일간 기간 타입을 처리해야 한다`() {
        // given
        val userStatEntities =
            listOf(
                createUserStatEntity(user, pet1, 100, 1),
            )

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(user.id, RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY)

        // then
        verify { userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.DAILY) }
        assertNotNull(result)
        assertEquals(1, result.rankings.size)
        assertEquals(1, result.myRanking.rank)
    }

    @Test
    fun `연간 기간 타입을 처리해야 한다`() {
        // given
        val userStatEntities =
            listOf(
                createUserStatEntity(user, pet1, 10000, 365),
            )

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(user.id, RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY)

        // then
        verify { userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_SUCCEEDED_DAYS, PeriodType.YEARLY) }
        assertNotNull(result)
        assertEquals(1, result.rankings.size)
        assertEquals(365, result.myRanking.userStat.totalSucceededDays)
    }

    @Test
    fun `대용량 데이터셋을 올바르게 처리해야 한다`() {
        // given
        val targetUser = createUser("targetUser")
        val targetPet = createPet(targetUser.id)

        val userStatEntities =
            (1..200).map { index ->
                val user = if (index == 150) targetUser else createUser("useruser")
                createUserStatEntity(user, pet, 2000 - index, index)
            }

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(targetUser.id, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        verify { userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY) }

        assertNotNull(result)
        assertEquals(200, result.rankings.size)

        // 내 랭킹 확인 (pet째사용자는 150위여야 함)
        assertEquals(150, result.myRanking.rank)
        assertEquals(targetUser.id, result.myRanking.userStat.user.id)

        // 상위 랭킹 제한 기능 확인
        val top10 = result.getTopRankings(10)
        assertEquals(10, top10.size)
        assertEquals(1, top10[0].rank)
        assertEquals(10, top10[9].rank)
    }

    @Test
    fun `서비스를 통해 동점 랭킹을 처리해야 한다`() {
        // given
        val userStatEntities =
            listOf(
                createUserStatEntity(user1, pet1, 1000, 10),
                createUserStatEntity(user2, pet2, 800, 15), // 사용자3과 동점
                createUserStatEntity(user3, pet3, 800, 20), // 사용자2와 동점
            )

        every {
            userStatRepository.findAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)
        } returns userStatEntities

        // when
        val result = rankingService.getRanking(user2.id, RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(2, result.rankings[1].rank) // 사용자2는 2위
        assertEquals(2, result.rankings[2].rank) // 사용자3도 2위 (동점)

        assertEquals(2, result.myRanking.rank)
        assertEquals(user2.id, result.myRanking.userStat.user.id)
    }

    private fun createUser(name: String): User = User.register("deviceToken-${name.lowercase()}", name)

    private fun createPet(ownerId: ObjectId): Pet = Pet.register(PetType.getRandom(), ownerId)

    private fun createUserStatEntity(
        user: User,
        mainPet: Pet,
        totalCalories: Int,
        totalSucceededDays: Int,
    ): UserStatEntity {
        val entity = mockk<UserStatEntity>()
        every { entity.toDomain() } returns
                UserStat(
                    user = user,
                    mainPet = mainPet,
                    totalCalories = totalCalories,
                    totalSucceededDays = totalSucceededDays,
                )
        return entity
    }
}
