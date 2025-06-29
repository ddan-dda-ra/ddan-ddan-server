package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.getAllRankingBy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val userStatRepository: UserStatRepository,
    private val rankingService: RankingService,
    private val pushClient: PushClient,
) {
    @Transactional(readOnly = true)
    fun notifyCheckCalorie() {
        val validDeviceTokens = getValidDeviceTokens(userRepository.findAll())

        pushClient.sendToMultiple(validDeviceTokens, PushMessage.CHECK_CALORIE, RoutingView.MAIN)
    }

    @Transactional(readOnly = true)
    fun notifyWeeklyRanking(totalCalories: Int) {
        val validDeviceTokens = getValidDeviceTokens(userRepository.findAll())

        pushClient.sendToMultiple(validDeviceTokens, PushMessage.weeklyRanking(totalCalories), RoutingView.MAIN)
    }

    @Transactional(readOnly = true)
    fun notifyRankingDiff() {
        val now = LocalDateTime.now()
        val currentRanking = userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.MONTHLY)
            .take(100)
        val currentRankingBoard =
            RankingBoard.of(
                id = PeriodType.MONTHLY,
                ranking = currentRanking,
            )

        val previousRankingBoard =
            runCatching { rankingService.getRankingBoard(PeriodType.MONTHLY) }
                .getOrElse {
                    rankingService.updateRankingBoard(currentRankingBoard)
                    return
                }

        // 매달 1일에는 갱신만 수행한다.
        if (now.dayOfMonth == 1) {
            rankingService.updateRankingBoard(currentRankingBoard)
            return
        }

        // 랭킹 차이가 있을 경우 푸시 알림
        previousRankingBoard.ranking.forEach { (previousRank, previousUserStat) ->
            val currentUserStat = currentRankingBoard.ranking.find { it.second.user.id == previousUserStat.user.id }
            // 현재 랭킹에 없거나 순위가 떨어진경우
            if (isRankingDown(currentUserStat, previousRank)) {
                userRepository
                    .findByIdOrNull(currentUserStat!!.second.user.id)
                    ?.takeIf { it.setting.isAppPushOn }
                    ?.deviceToken?.takeIf { it.isValid() }
                    ?.let { pushClient.sendToUser(it.value, PushMessage.RANKING_DOWN, RoutingView.MAIN) }
            }
        }
        rankingService.updateRankingBoard(currentRankingBoard)
    }

    private fun getValidDeviceTokens(users: List<User>): List<String> {
        return users
            .filter { it.setting.isAppPushOn }
            .mapNotNull { user -> user.deviceToken?.takeIf { it.isValid() }?.value }
    }

    private fun isRankingDown(
        currentUserStat: Pair<Int, UserStat>?,
        previousRank: Int,
    ) = (currentUserStat == null || currentUserStat.first > previousRank)
}
