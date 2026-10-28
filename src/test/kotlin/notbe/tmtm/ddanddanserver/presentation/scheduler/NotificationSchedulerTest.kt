package notbe.tmtm.ddanddanserver.presentation.scheduler

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.NotificationService
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationSchedulerTest : FunSpec({
    test("새 펫 알림은 실행 시각과 관계없이 오늘 14:00 KST를 집계 기준 시각으로 넘긴다") {
        val notificationService = mockk<NotificationService>(relaxed = true)
        val scheduler = NotificationScheduler(notificationService, mockk())
        val seoul = ZoneId.of("Asia/Seoul")
        val expected = ZonedDateTime.now(seoul).with(LocalTime.of(14, 0)).toInstant()

        scheduler.notifyNewPetArrived()

        val until = slot<Instant>()
        verify(exactly = 1) { notificationService.notifyNewPetArrived(capture(until)) }
        until.captured shouldBe expected
    }
})
