package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.NotificationService
import notbe.tmtm.ddanddanserver.application.service.RankingService
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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

    @Scheduled(cron = "0 0 14 * * *", zone = "Asia/Seoul") // 매일 14:00
    fun notifyNewPetArrived() {
        // 실행이 조금 늦어도 집계 구간이 밀리지 않도록 오늘 14:00 정각을 기준으로 한다.
        val until = ZonedDateTime.now(SEOUL).with(NEW_PET_NOTIFY_TIME).toInstant()
        notificationService.notifyNewPetArrived(until)
    }

    companion object {
        private val SEOUL = ZoneId.of("Asia/Seoul")
        private val NEW_PET_NOTIFY_TIME = LocalTime.of(14, 0)
    }
}
