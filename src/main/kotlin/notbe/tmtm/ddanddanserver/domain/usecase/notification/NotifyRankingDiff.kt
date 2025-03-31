package notbe.tmtm.ddanddanserver.domain.usecase.notification

import notbe.tmtm.ddanddanserver.domain.gateway.PushGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserStatGateway
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.ranking.GetRankingBoard
import notbe.tmtm.ddanddanserver.domain.usecase.ranking.UpdateRankingBoard
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDateTime

@Component
class NotifyRankingDiff(
    private val userStatGateway: UserStatGateway,
    private val getRankingBoard: GetRankingBoard,
    private val updateRankingBoard: UpdateRankingBoard,
    private val pushGateway: PushGateway,
    private val userGateway: UserGateway,
) : UseCase<Unit, Unit> {
    override fun execute(input: Unit) {
        val now = LocalDateTime.now()
        val previousRankingBoard = getRankingBoard.execute(PeriodType.WEEKLY)
        val currentRanking = userStatGateway.getRanking(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY).take(100)
        val currentRankingBoard = RankingBoard.of(
            id = PeriodType.WEEKLY,
            ranking = currentRanking,
        )

        // 첫 주간 랭킹은 갱신만 수행
        if (now.dayOfWeek == DayOfWeek.MONDAY && now.hour in 0..11) {
            updateRankingBoard.execute(currentRankingBoard)
            return
        }

        // 랭킹 차이가 있을 경우 푸시 알림
        previousRankingBoard.ranking.forEach { (previousRank, previousUserStat) ->
            val currentUserStat = currentRankingBoard.ranking.find { it.second.userId == previousUserStat.userId }
            // 현재 랭킹에 없거나 순위가 떨어진경우
            if (isRankingDown(currentUserStat, previousRank)) {
                userGateway.getById(currentUserStat!!.second.userId).deviceToken?.let { deviceToken ->
                    pushGateway.send(deviceToken, "\uD83D\uDCC9  순위가 떨어졌어요!", RoutingView.MAIN)
                }
            }
        }
        updateRankingBoard.execute(currentRankingBoard)
    }

    private fun isRankingDown(
        currentUserStat: Pair<Int, UserStat>?,
        previousRank: Int
    ) = (currentUserStat != null && currentUserStat.first >= previousRank).not()
}
