package notbe.tmtm.ddanddanserver.domain.usecase.ranking

import notbe.tmtm.ddanddanserver.domain.gateway.UserStatGateway
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private const val RANKING_LIMIT = 100

@Component
class GetRanking(
    private val userStatGateway: UserStatGateway,
) : UseCase<GetRanking.Input, GetRanking.Output> {
    data class Input(
        val userId: String,
        val criteria: RankingCriteria,
        val periodType: PeriodType,
    )

    data class Output(
        val userStats: List<Pair<UserStat, Int>>,
        val myStats: Pair<UserStat, Int>,
    )

    @Transactional(readOnly = true)
    override fun execute(input: Input): Output {
        val userStats = userStatGateway.getRanking(input.criteria, input.periodType)
        val userStatsWithRank =
            when (input.criteria) {
                RankingCriteria.TOTAL_CALORIES -> getRankingList(userStats) { it.totalCalories }
                RankingCriteria.TOTAL_SUCCEEDED_DAYS -> getRankingList(userStats) { it.totalSucceededDays }
            }

        return Output(userStatsWithRank.take(RANKING_LIMIT), getMyStats(userStatsWithRank, input.userId))
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
        userId: String,
    ): Pair<UserStat, Int> {
        val myStats = userStats.find { it.first.userId == userId }!!
        return myStats
    }
}
