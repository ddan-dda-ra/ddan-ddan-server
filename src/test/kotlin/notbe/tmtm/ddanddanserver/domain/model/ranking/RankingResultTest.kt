package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.exception.NotFoundUserStatException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RankingResultTest {
    private val user1 = createUser("user1")
    private val pet1 = createPet(user1.id)
    private val user2 = createUser("user2")
    private val pet2 = createPet(user2.id)
    private val user3 = createUser("user3")
    private val pet3 = createPet(user3.id)
    private val user4 = createUser("user4")
    private val pet4 = createPet(user4.id)

    @Test
    fun `총 칼로리 기준으로 올바른 순서의 랭킹을 생성해야 한다`() {
        // given
        val userStats =
            listOf(
                createUserStat(user1, pet1, 1000, 10), // 1위
                createUserStat(user2, pet2, 800, 15), // 2위
                createUserStat(user3, pet3, 600, 20), // 3위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, user2.id)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(3, result.rankings[2].rank)
        assertEquals(user3, result.rankings[2].userStat.user)

        // 내 랭킹은 사용자2로 2위여야 함
        assertEquals(2, result.myRanking.rank)
        assertEquals(user2, result.myRanking.userStat.user)
    }

    @Test
    fun `총 성공일 기준으로 올바른 순서의 랭킹을 생성해야 한다`() {
        // given
        val userStats =
            listOf(
                createUserStat(user1, pet1, 500, 25), // 1위 (가장 많은 성공일)
                createUserStat(user2, pet2, 1000, 20), // 2위
                createUserStat(user3, pet3, 800, 15), // 3위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_SUCCEEDED_DAYS, user1.id)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(3, result.rankings[2].rank)
        assertEquals(user3, result.rankings[2].userStat.user)

        // 내 랭킹은 사용자1로 1위여야 함
        assertEquals(1, result.myRanking.rank)
        assertEquals(user1, result.myRanking.userStat.user)
    }

    @Test
    fun `총 출석일 기준으로 올바른 순서의 랭킹을 생성해야 한다`() {
        // given - 출석일 5 / 3 / 1 순서로 정렬되어야 한다
        val userStats =
            listOf(
                createUserStat(user1, pet1, 100, 1, totalAttendanceDays = 5), // 1위
                createUserStat(user2, pet2, 100, 1, totalAttendanceDays = 3), // 2위
                createUserStat(user3, pet3, 100, 1, totalAttendanceDays = 1), // 3위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_ATTENDANCE_DAYS, user2.id)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(3, result.rankings[2].rank)
        assertEquals(user3, result.rankings[2].userStat.user)

        assertEquals(2, result.myRanking.rank)
        assertEquals(user2, result.myRanking.userStat.user)
    }

    @Test
    fun `동점자가 있는 랭킹을 올바르게 처리해야 한다`() {
        // given
        val userStats =
            listOf(
                createUserStat(user1, pet1, 1000, 10), // 1위
                createUserStat(user2, pet2, 800, 15), // 2위 (동점)
                createUserStat(user3, pet3, 800, 20), // 2위 (동점)
                createUserStat(user4, pet4, 600, 25), // 4위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, user3.id)

        // then
        assertEquals(4, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1, result.rankings[0].userStat.user)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(user2, result.rankings[1].userStat.user)
        assertEquals(2, result.rankings[2].rank) // 동점으로 같은 순위
        assertEquals(user3, result.rankings[2].userStat.user)
        assertEquals(4, result.rankings[3].rank) // 3순위는 건너뜨
        assertEquals(user4, result.rankings[3].userStat.user)

        // 내 랭킹은 사용자3로 2위여야 함 (동점)
        assertEquals(2, result.myRanking.rank)
        assertEquals(user3, result.myRanking.userStat.user)
    }

    @Test
    fun `랭킹에서 사용자를 찾을 수 없을 때 예외를 발생시켜야 한다`() {
        // given
        val nonExistentUser = createUser("nonExistentUser")
        val nonExistentPet = createPet(nonExistentUser.id)

        val userStats =
            listOf(
                createUserStat(user1, pet1, 1000, 10),
                createUserStat(user2, pet2, 800, 15),
            )

        // when & then
        assertThrows<NotFoundUserStatException> {
            RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, nonExistentUser.id)
        }
    }

    @Test
    fun `제한된 개수의 상위 랭킹을 반환해야 한다`() {
        // given
        val userStats =
            (1..150).map { index ->
                createUserStat(user1, pet1, 1000 - index, index)
            }
        val firstUserId = userStats[0].user.id

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, firstUserId)
        val top10 = result.getTopRankings(10)

        // then
        assertEquals(10, top10.size)
        assertEquals(150, result.rankings.size) // 모든 랭킹이 보존됨

        // 상위 10개가 올바른 순서인지 확인
        for (i in 0 until 10) {
            assertEquals(i + 1, top10[i].rank)
        }
    }

    @Test
    fun `랭킹 순서의 불변성을 보장해야 한다`() {
        // given
        val userStats =
            listOf(
                createUserStat(user1, pet1, 1000, 10),
                createUserStat(user2, pet2, 800, 15),
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, user1.id)
        val rankings1 = result.rankings
        val rankings2 = result.rankings

        // then
        assertNotNull(rankings1)
        assertNotNull(rankings2)
        assertTrue(rankings1 !== rankings2) // 다른 인스턴스 (방어적 복사)
        assertEquals(rankings1.size, rankings2.size)
        assertEquals(rankings1[0].rank, rankings2[0].rank)
    }

    @Test
    fun `빈 사용자 통계 목록을 처리해야 한다`() {
        // given
        val emptyUserStats = emptyList<UserStat>()
        val userId = ObjectId()

        // when & then
        assertThrows<NotFoundUserStatException> {
            RankingResult.create(emptyUserStats, RankingCriteria.TOTAL_CALORIES, userId)
        }
    }

    @Test
    fun `단일 사용자를 올바르게 처리해야 한다`() {
        // given
        val userStats =
            listOf(
                createUserStat(user1, pet1, 1000, 10),
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, user1.id)

        // then
        assertEquals(1, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(user1.id, result.rankings[0].userStat.user.id)
        assertEquals(1, result.myRanking.rank)
        assertEquals(user1.id, result.myRanking.userStat.user.id)
    }

    private fun createUser(name: String): User = User.register("deviceToken-${name.lowercase()}", name)

    private fun createPet(ownerId: ObjectId): Pet = Pet.register(listOf("CAT", "DOG", "HAMSTER", "PENGUIN", "MOLE").random(), ownerId)

    private fun createUserStat(
        user: User,
        mainPet: Pet,
        totalCalories: Int,
        totalSucceededDays: Int,
        totalAttendanceDays: Int = 0,
    ): UserStat {
        return UserStat(
            user = user,
            mainPet = mainPet,
            totalCalories = totalCalories,
            totalSucceededDays = totalSucceededDays,
            totalAttendanceDays = totalAttendanceDays,
        )
    }
}
