package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.NotificationService
import notbe.tmtm.ddanddanserver.application.service.RankingService
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notificationService: NotificationService,
    private val rankingService: RankingService,
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
        val topRanking =
            rankingService.getTopRanking(RankingCriteria.TOTAL_CALORIES, PeriodType.WEEKLY) ?: return
        notificationService.notifyWeeklyRanking(topRanking.totalCalories)
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul") // 매일 12:00
    fun notifyRankingDiff() {
        notificationService.notifyRankingDiff()
    }
}
