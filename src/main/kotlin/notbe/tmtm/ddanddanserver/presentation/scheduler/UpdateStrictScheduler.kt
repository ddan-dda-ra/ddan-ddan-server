package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.DailyInfoService
import notbe.tmtm.ddanddanserver.application.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UpdateStrictScheduler(
    private val userService: UserService,
    private val dailyInfoService: DailyInfoService,
) {
    @Scheduled(cron = "1 0 0 * * *", zone = "Asia/Seoul") // 매일 00:00:01
    fun updatePurposeStrict() {
        val goalYesterdayUserIds =
            dailyInfoService
                .getAllPurposeSuccess(LocalDate.now().minusDays(1))
                .map { it.userId }

        // 어제 목표를 달성하지 못한 유저의 purpose_strict만 0으로 바꾼다. 문서 전체를 저장하지 않으므로 다른 필드를 덮어쓰지 않는다.
        userService.resetPurposeStrictExcept(goalYesterdayUserIds)
    }
}
