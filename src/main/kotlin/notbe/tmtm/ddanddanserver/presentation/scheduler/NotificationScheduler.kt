package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.NotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notificationService: NotificationService,
) {
    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Seoul") // 매일 20:30
    fun notifySleepingUsers() {
        // 추후 진행
//        notificationService.notifySleepingUsers()
    }

    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Seoul") // 매일 20:30
    fun notifyCheckRankingMessage() {
        notificationService.notifyCheckCalorie()
    }

    @Scheduled(cron = "0 30 8 * * 5", zone = "Asia/Seoul") // 매주 금요일 08:30
    fun notifyWeeklyRanking() {
        notificationService.notifyWeeklyRanking()
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul") // 매일 12:00
    fun notifyRankingDiff() {
        notificationService.notifyRankingDiff()
    }
}
