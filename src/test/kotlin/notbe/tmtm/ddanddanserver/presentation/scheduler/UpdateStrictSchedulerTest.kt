package notbe.tmtm.ddanddanserver.presentation.scheduler

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.DailyInfoService
import notbe.tmtm.ddanddanserver.application.service.UserService
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import org.bson.types.ObjectId
import java.time.LocalDate

class UpdateStrictSchedulerTest : FunSpec({
    lateinit var userService: UserService
    lateinit var dailyInfoService: DailyInfoService
    lateinit var scheduler: UpdateStrictScheduler

    beforeEach {
        userService = mockk()
        dailyInfoService = mockk()
        scheduler = UpdateStrictScheduler(userService, dailyInfoService)
    }

    test("어제 목표를 달성한 유저를 제외하고 연속 달성을 일괄 초기화해야 한다") {
        val yesterday = LocalDate.now().minusDays(1)
        val achieved = listOf(
            DailyInfo.create(ObjectId(), "a", "DOG", yesterday),
            DailyInfo.create(ObjectId(), "b", "CAT", yesterday),
        )
        every { dailyInfoService.getAllPurposeSuccess(yesterday) } returns achieved
        every { userService.resetPurposeStrictExcept(any()) } returns 1L

        scheduler.updatePurposeStrict()

        verify(exactly = 1) { userService.resetPurposeStrictExcept(achieved.map { it.userId }) }
        verify(exactly = 0) { userService.getAll() }
    }
})
