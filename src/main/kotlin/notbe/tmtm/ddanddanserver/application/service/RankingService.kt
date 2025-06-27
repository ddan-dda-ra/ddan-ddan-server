package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.NotFoundUserStatException
import notbe.tmtm.ddanddanserver.domain.model.ranking.*
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val RANKING_LIMIT = 100

@Service
class RankingService(
    private val userStatRepository: UserStatRepository,
    private val rankingBoardRepository: RankingBoardRepository,
) {
    @Transactional(readOnly = true)
    fun getRanking(
        userId: ObjectId,
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): RankingResult {
        val userStats = userStatRepository.getAllRankingBy(criteria, periodType)
        return RankingResult.create(userStats, criteria, userId)
    }

    @Transactional(readOnly = true)
    fun getRankingWithRank(
        userId: ObjectId,
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): Pair<List<Pair<UserStat, Int>>, Pair<UserStat, Int>> {
        val userStats = userStatRepository.getAllRankingBy(criteria, periodType)
        val userStatsWithRank = when (criteria) {
            RankingCriteria.TOTAL_CALORIES -> getRankingList(userStats) { it.totalCalories }
            RankingCriteria.TOTAL_SUCCEEDED_DAYS -> getRankingList(userStats) { it.totalSucceededDays }
        }

        return Pair(userStatsWithRank.take(RANKING_LIMIT), getMyStats(userStatsWithRank, userId))
    }

    @Transactional(readOnly = true)
    fun getRankingBoard(periodType: PeriodType): RankingBoard {
        return rankingBoardRepository.findByIdOrDefault(periodType)
    }

    @Transactional(readOnly = true)
    fun getTopRanking(criteria: RankingCriteria, periodType: PeriodType): UserStat? {
        val userStats: List<UserStat> = userStatRepository.getAllRankingBy(criteria, periodType)
        return if (userStats.isEmpty()) null else userStats.first()
    }

    @Transactional
    fun updateRankingBoard(rankingBoard: RankingBoard): RankingBoard {
        return rankingBoardRepository.save(rankingBoard)
    }

    private fun getRankingList(
        userStats: List<UserStat>,
        field: (UserStat) -> Int,
    ): List<Pair<UserStat, Int>> =
        userStats
            .foldIndexed(emptyList<Pair<UserStat, Int>>() to 1) { index, (acc, lastRank), user ->
                val rank = if (index > 0 && field(user) == field(acc.last().first)) lastRank else index + 1
                (acc + (user to rank)) to rank
            }.first

    private fun getMyStats(
        userStats: List<Pair<UserStat, Int>>,
        userId: ObjectId,
    ): Pair<UserStat, Int> {
        val myStats = userStats.find { it.first.userId == userId }
            ?: throw NotFoundUserStatException("갱신하지 않은 사용자입니다. userId=$userId")
        return myStats
    }
}
