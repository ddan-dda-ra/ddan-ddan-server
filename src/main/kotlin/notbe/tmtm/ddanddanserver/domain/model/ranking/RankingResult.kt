package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.exception.NotFoundUserStatException
import org.bson.types.ObjectId

data class RankingResult private constructor(
    private val sortedRankings: List<UserRanking>,
    val myRanking: UserRanking,
) {
    val rankings: List<UserRanking>
        get() = sortedRankings.toList()

    fun getTopRankings(limit: Int): List<UserRanking> = sortedRankings.take(limit)

    companion object {
        fun create(
            userStats: List<UserStat>,
            criteria: RankingCriteria,
            userId: ObjectId,
        ): RankingResult {
            val sortedUserRankings = calculateRankings(userStats, criteria)
            val myRanking = findMyRanking(sortedUserRankings, userId)

            return RankingResult(sortedUserRankings, myRanking)
        }

        private fun calculateRankings(
            userStats: List<UserStat>,
            criteria: RankingCriteria,
        ): List<UserRanking> {
            val fieldExtractor: (UserStat) -> Int =
                when (criteria) {
                    RankingCriteria.TOTAL_CALORIES -> { it -> it.totalCalories }
                    RankingCriteria.TOTAL_SUCCEEDED_DAYS -> { it -> it.totalSucceededDays }
                }

            return userStats
                .foldIndexed(emptyList<UserRanking>() to 1) { index, (acc, lastRank), userStat ->
                    val rank =
                        if (index > 0 && fieldExtractor(userStat) == fieldExtractor(acc.last().userStat)) {
                            lastRank
                        } else {
                            index + 1
                        }
                    (acc + UserRanking(userStat, rank)) to rank
                }.first
        }

        private fun findMyRanking(
            rankings: List<UserRanking>,
            userId: ObjectId,
        ): UserRanking =
            rankings.find { it.userStat.userId == userId }
                ?: throw NotFoundUserStatException("갱신하지 않은 사용자입니다. userId=$userId")
    }
}
