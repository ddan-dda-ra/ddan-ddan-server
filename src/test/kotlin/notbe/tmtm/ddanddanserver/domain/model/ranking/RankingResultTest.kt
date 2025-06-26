package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.exception.NotFoundUserStatException
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RankingResultTest {
    @Test
    fun `총 칼로리 기준으로 올바른 순서의 랭킹을 생성해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()

        val userStats =
            listOf(
                createUserStat(userId1, "사용자1", 1000, 10), // 1위
                createUserStat(userId2, "사용자2", 800, 15), // 2위
                createUserStat(userId3, "사용자3", 600, 20), // 3위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, userId2)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId1, result.rankings[0].userStat.userId)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(userId2, result.rankings[1].userStat.userId)
        assertEquals(3, result.rankings[2].rank)
        assertEquals(userId3, result.rankings[2].userStat.userId)

        // 내 랭킹은 사용자2로 2위여야 함
        assertEquals(2, result.myRanking.rank)
        assertEquals(userId2, result.myRanking.userStat.userId)
    }

    @Test
    fun `총 성공일 기준으로 올바른 순서의 랭킹을 생성해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()

        val userStats =
            listOf(
                createUserStat(userId1, "사용자1", 500, 25), // 1위 (가장 많은 성공일)
                createUserStat(userId2, "사용자2", 1000, 20), // 2위
                createUserStat(userId3, "사용자3", 800, 15), // 3위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_SUCCEEDED_DAYS, userId1)

        // then
        assertEquals(3, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId1, result.rankings[0].userStat.userId)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(userId2, result.rankings[1].userStat.userId)
        assertEquals(3, result.rankings[2].rank)
        assertEquals(userId3, result.rankings[2].userStat.userId)

        // 내 랭킹은 사용자1로 1위여야 함
        assertEquals(1, result.myRanking.rank)
        assertEquals(userId1, result.myRanking.userStat.userId)
    }

    @Test
    fun `동점자가 있는 랭킹을 올바르게 처리해야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val userId3 = ObjectId()
        val userId4 = ObjectId()

        val userStats =
            listOf(
                createUserStat(userId1, "사용자1", 1000, 10), // 1위
                createUserStat(userId2, "사용자2", 800, 15), // 2위 (동점)
                createUserStat(userId3, "사용자3", 800, 20), // 2위 (동점)
                createUserStat(userId4, "사용자4", 600, 25), // 4위
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, userId3)

        // then
        assertEquals(4, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId1, result.rankings[0].userStat.userId)
        assertEquals(2, result.rankings[1].rank)
        assertEquals(userId2, result.rankings[1].userStat.userId)
        assertEquals(2, result.rankings[2].rank) // 동점으로 같은 순위
        assertEquals(userId3, result.rankings[2].userStat.userId)
        assertEquals(4, result.rankings[3].rank) // 3순위는 건너뜨
        assertEquals(userId4, result.rankings[3].userStat.userId)

        // 내 랭킹은 사용자3로 2위여야 함 (동점)
        assertEquals(2, result.myRanking.rank)
        assertEquals(userId3, result.myRanking.userStat.userId)
    }

    @Test
    fun `랭킹에서 사용자를 찾을 수 없을 때 예외를 발생시켜야 한다`() {
        // given
        val userId1 = ObjectId()
        val userId2 = ObjectId()
        val nonExistentUserId = ObjectId()

        val userStats =
            listOf(
                createUserStat(userId1, "사용자1", 1000, 10),
                createUserStat(userId2, "사용자2", 800, 15),
            )

        // when & then
        assertThrows<NotFoundUserStatException> {
            RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, nonExistentUserId)
        }
    }

    @Test
    fun `제한된 개수의 상위 랭킹을 반환해야 한다`() {
        // given
        val userStats =
            (1..150).map { index ->
                createUserStat(ObjectId(), "사용자$index", 1000 - index, index)
            }
        val firstUserId = userStats[0].userId

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
        val userId1 = ObjectId()
        val userId2 = ObjectId()

        val userStats =
            listOf(
                createUserStat(userId1, "사용자1", 1000, 10),
                createUserStat(userId2, "사용자2", 800, 15),
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, userId1)
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
        val userId = ObjectId()
        val userStats =
            listOf(
                createUserStat(userId, "사용자1", 1000, 10),
            )

        // when
        val result = RankingResult.create(userStats, RankingCriteria.TOTAL_CALORIES, userId)

        // then
        assertEquals(1, result.rankings.size)
        assertEquals(1, result.rankings[0].rank)
        assertEquals(userId, result.rankings[0].userStat.userId)
        assertEquals(1, result.myRanking.rank)
        assertEquals(userId, result.myRanking.userStat.userId)
    }

    private fun createUserStat(
        userId: ObjectId,
        userName: String,
        totalCalories: Int,
        totalSucceededDays: Int,
    ) = UserStat(
        userId = userId,
        userName = userName,
        mainPetType = PetType.DOG,
        totalCalories = totalCalories,
        totalSucceededDays = totalSucceededDays,
    )
}
