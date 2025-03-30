package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifyCheckCalorie
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
    private val getTopRanking: GetTopRanking,
) {
    @Scheduled(cron = "0 30 20 * * *") // 매일 20:30
    fun notifySleepingUsers() {
        notifySleepingUsers.execute(Unit)
    }

    @Scheduled(cron = "0 30 20 * * *") // 매일 20:30
    fun notifyCheckRankingMessage() {
        notifyCheckCalorie.execute(Unit)
    }

    @Scheduled(cron = "0 30 20 * * 5") // 매주 금요일 20:30
    fun notifyWeeklyRanking() {
        val topRanking =
            getTopRanking.execute(GetTopRanking.Input(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY)).user ?: return
        notifyWeeklyRanking.execute(topRanking.totalCalories)
    }
}
