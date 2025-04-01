package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifyCheckCalorie
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifyRankingDiff
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifySleepingUsers
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifyWeeklyRanking
import notbe.tmtm.ddanddanserver.domain.usecase.ranking.GetTopRanking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notifySleepingUsers: NotifySleepingUsers,
    private val notifyCheckCalorie: NotifyCheckCalorie,
    private val notifyWeeklyRanking: NotifyWeeklyRanking,
    private val notifyRankingDiff: NotifyRankingDiff,
    private val getTopRanking: GetTopRanking,
) {
    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Seoul") // 매일 20:30
    fun notifySleepingUsers() {
        // 추후 진행
//        notifySleepingUsers.execute(Unit)
    }

    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Seoul") // 매일 20:30
    fun notifyCheckRankingMessage() {
        notifyCheckCalorie.execute(Unit)
    }

    @Scheduled(cron = "0 30 8 * * 5", zone = "Asia/Seoul") // 매주 금요일 08:30
    fun notifyWeeklyRanking() {
        val topRanking =
            getTopRanking.execute(GetTopRanking.Input(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)).user ?: return
        notifyWeeklyRanking.execute(topRanking.totalCalories)
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul") // 매일 12:00
    fun notifyRankingDiff() {
        notifyRankingDiff.execute(Unit)
    }
}
