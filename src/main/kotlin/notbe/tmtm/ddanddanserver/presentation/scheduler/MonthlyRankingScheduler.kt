package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.MonthlyRankingService
import notbe.tmtm.ddanddanserver.common.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MonthlyRankingScheduler(
    private val monthlyRankingService: MonthlyRankingService,
) {
    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul") // 매월 1일 00:05
    fun sendPreviousMonthRanking() {
        logger().info("월간 랭킹 발송 스케줄 시작")
        monthlyRankingService.sendPreviousMonthRanking()
    }
}
