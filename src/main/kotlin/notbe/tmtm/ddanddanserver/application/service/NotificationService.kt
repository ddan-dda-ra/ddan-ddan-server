package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.gateway.FcmPushAdapter
import notbe.tmtm.ddanddanserver.infrastructure.gateway.RankingBoardAdapter
import notbe.tmtm.ddanddanserver.infrastructure.gateway.UserStatAdapter
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.abs

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val fcmPushAdapter: FcmPushAdapter,
    private val userStatAdapter: UserStatAdapter,
    private val rankingBoardAdapter: RankingBoardAdapter,
) {
    val logger = logger()

    fun notifySleepingUsers() {
        // TODO: 구현 필요
    }

    fun notifyCheckCalorie() {
        val users = userRepository.findAll()
        users.forEach { user ->
            val dailyInfo = dailyInfoRepository.findByUserIdAndDate(user.id, LocalDate.now())
            if (dailyInfo == null || dailyInfo.calorie < user.purposeCalorie) {
                fcmPushAdapter.send(
                    deviceToken = user.deviceToken,
                    content = PushMessage.CHECK_CALORIE.message!!,
                    routingView = RoutingView.MAIN,
                )
            }
        }
    }

    fun notifyWeeklyRanking() {
        val topRanking = userStatAdapter.getRanking(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY).firstOrNull()
        topRanking?.let {
            val users = userRepository.findAll()
            users.forEach { user ->
                fcmPushAdapter.send(
                    deviceToken = user.deviceToken,
                    content = PushMessage.WEEKLY_RANKING.message!!.format(it.totalCalories),
                    routingView = RoutingView.RANKING,
                )
            }
        }
    }

    fun notifyRankingDiff() {
        val users = userRepository.findAll()
        users.forEach { user ->
            val currentRanking = userStatAdapter.getRanking(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY).indexOfFirst { it.userId == user.id } + 1
            val previousRanking = rankingBoardAdapter.getById(PeriodType.WEEKLY).ranking.indexOfFirst { it.second.userId == user.id } + 1

            if (currentRanking != previousRanking) {
                val diff = previousRanking - currentRanking
                val message = if (diff > 0) PushMessage.RANKING_UP.message!!.format(diff) else PushMessage.RANKING_DOWN.message!!.format(abs(diff))
                fcmPushAdapter.send(
                    deviceToken = user.deviceToken,
                    content = message,
                    routingView = RoutingView.RANKING,
                )
            }
        }
    }
}
