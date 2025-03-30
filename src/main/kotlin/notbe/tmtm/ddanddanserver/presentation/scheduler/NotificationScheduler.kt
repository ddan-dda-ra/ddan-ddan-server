package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifyCheckCalorie
import notbe.tmtm.ddanddanserver.domain.usecase.notification.NotifySleepingUsers
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notifySleepingUsers: NotifySleepingUsers,
    private val notifyCheckCalorie: NotifyCheckCalorie,
) {
    @Scheduled(cron = "0 30 20 * * *") // 매일 20:30
    fun notifySleepingUsers() {
        notifySleepingUsers.execute(Unit)
    }

    @Scheduled(cron = "0 30 20 * * *") // 매일 20:30
    fun notifyCheckRankingMessage() {
        notifyCheckCalorie.execute(Unit)
    }
}
