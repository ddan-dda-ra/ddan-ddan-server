package notbe.tmtm.ddanddanserver.application.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
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
    private val fcmClient: FirebaseMessaging,
) {
    @Transactional(readOnly = true)
    fun notifyCheckCalorie() {
        val allUsers = userRepository.findAll()
            .filter { it.setting.isAppPushOn }
            .filter { it.deviceToken.isNullOrEmpty().not() }

        sendPushToAllUsers(allUsers.map { it.deviceToken!! }, PushMessage.CHECK_CALORIE.content, RoutingView.MAIN)
    }

    @Transactional(readOnly = true)
    fun notifyWeeklyRanking(totalCalories: Int) {
        val allUsers = userRepository.findAll()
            .filter { it.setting.isAppPushOn }
            .filter { it.deviceToken.isNullOrEmpty().not() }

        sendPushToAllUsers(
            allUsers.map { it.deviceToken!! },
            "${PushMessage.WEEKLY_RANKING.content} $totalCalories 칼로리를 소모했대요.",
            RoutingView.MAIN
        )
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
                    .findByIdOrNull(currentUserStat!!.second.user.id)!!
                    .takeIf { it.setting.isAppPushOn && it.deviceToken != null }
                    ?.let { sendPushToUser(it.deviceToken!!, "\uD83D\uDCC9  순위가 떨어졌어요!", RoutingView.MAIN) }
            }
        }
        rankingService.updateRankingBoard(currentRankingBoard)
    }

    private fun sendPushToAllUsers(deviceTokens: List<String>, message: String, routingView: RoutingView) {
        if (deviceTokens.isEmpty()) return

        val requests = deviceTokens
            .filter { it != "deviceToken" }
            .map { token ->
                Message
                    .builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setBody(message).build())
                    .putData("routingView", routingView.name)
                    .build()
            }

        fcmClient.sendEach(requests)
    }

    private fun sendPushToUser(deviceToken: String, message: String, routingView: RoutingView) {
        if (deviceToken == "deviceToken") return

        val request = Message
            .builder()
            .setToken(deviceToken)
            .setNotification(Notification.builder().setBody(message).build())
            .putData("routingView", routingView.name)
            .build()

        fcmClient.send(request)
    }

    private fun isRankingDown(
        currentUserStat: Pair<Int, UserStat>?,
        previousRank: Int,
    ) = (currentUserStat == null || currentUserStat.first > previousRank)
}
