package notbe.tmtm.ddanddanserver.presentation.scheduler

import notbe.tmtm.ddanddanserver.application.service.DailyInfoService
import notbe.tmtm.ddanddanserver.application.service.UserService
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
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
        val allUsers = userService.getAll()
        val goalYesterdayUserIds =
            dailyInfoService
                .getAllPurposeSuccess(LocalDate.now().minusDays(1))
                .map { it.userId }

        allUsers
            .filter { user -> didFailGoalYesterday(goalYesterdayUserIds, user) }
            .forEach { user ->
                user.purposeStrict = 0
                userService.update(user)
            }
    }

    private fun didFailGoalYesterday(
        goalUserIds: List<ObjectId>,
        user: User,
    ) = goalUserIds.contains(user.id).not()
}
