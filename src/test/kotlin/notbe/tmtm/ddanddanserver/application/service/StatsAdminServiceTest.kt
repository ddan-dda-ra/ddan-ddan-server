package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

import notbe.tmtm.ddanddanserver.infrastructure.database.repository.StatsAdminRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.StatsDailyCount
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.StatsPetTypeCount
import java.time.LocalDate

class StatsAdminServiceTest : FunSpec({
    lateinit var repository: StatsAdminRepository
    lateinit var service: StatsAdminService

    beforeEach {
        repository = mockk()
        service = StatsAdminService(repository)
    }

    test("getDashboard는 repository 호출 결과를 그대로 응답에 매핑한다") {
        every { repository.countAllUsers() } returns 1284L
        every { repository.countAllPets() } returns 956L
        every { repository.countNewUsersBetween(any(), any()) } returnsMany
            listOf(17L, 142L, 612L)
        every { repository.countDistinctActiveUsersBetween(any(), any()) } returnsMany
            listOf(412L, 893L)
        every { repository.groupPetsByType() } returns
            listOf(
                StatsPetTypeCount("CAT", 312),
                StatsPetTypeCount("DOG", 280),
                StatsPetTypeCount("HAMSTER", 200),
            )
        every { repository.getSignupSeries(any(), any()) } returns
            listOf(
                StatsDailyCount("2026-04-22", 17),
                StatsDailyCount("2026-04-23", 22),
            )

        val result = service.getDashboard(seriesDays = 14)

        result.totalUsers shouldBe 1284L
        result.totalPets shouldBe 956L
        result.newUsersToday shouldBe 17L
        result.newUsersThisWeek shouldBe 142L
        result.newUsersThisMonth shouldBe 612L
        result.activeUsersToday shouldBe 412L
        result.activeUsersThisWeek shouldBe 893L
        result.petDistribution shouldHaveSize 3
        result.petDistribution[0].type shouldBe "CAT"
        result.petDistribution[0].count shouldBe 312L
        result.signupSeries.map { it.date } shouldContainExactly listOf("2026-04-22", "2026-04-23")
    }

    test("today 호출은 from=to=오늘로 일치한다") {
        every { repository.countAllUsers() } returns 0L
        every { repository.countAllPets() } returns 0L
        every { repository.countDistinctActiveUsersBetween(any(), any()) } returns 0L
        every { repository.groupPetsByType() } returns emptyList()
        every { repository.getSignupSeries(any(), any()) } returns emptyList()

        val fromCaptors = mutableListOf<LocalDate>()
        val toCaptors = mutableListOf<LocalDate>()
        every { repository.countNewUsersBetween(capture(fromCaptors), capture(toCaptors)) } returns 0L

        service.getDashboard(seriesDays = 14)

        // 첫 번째 호출이 오늘(today, today)
        fromCaptors[0] shouldBe toCaptors[0]
        // 두 번째 호출이 7일 (오늘 포함, weekAgo = today-6)
        toCaptors[1] shouldBe fromCaptors[0]
        java.time.temporal.ChronoUnit.DAYS.between(fromCaptors[1], toCaptors[1]) shouldBe 6L
        // 세 번째 호출이 30일 (오늘 포함, monthAgo = today-29)
        java.time.temporal.ChronoUnit.DAYS.between(fromCaptors[2], toCaptors[2]) shouldBe 29L
    }

    test("seriesDays=N이면 시리즈 시작일은 오늘에서 N-1일 전") {
        every { repository.countAllUsers() } returns 0L
        every { repository.countAllPets() } returns 0L
        every { repository.countNewUsersBetween(any(), any()) } returns 0L
        every { repository.countDistinctActiveUsersBetween(any(), any()) } returns 0L
        every { repository.groupPetsByType() } returns emptyList()

        val seriesFromSlot = slot<LocalDate>()
        val seriesToSlot = slot<LocalDate>()
        every { repository.getSignupSeries(capture(seriesFromSlot), capture(seriesToSlot)) } returns emptyList()

        service.getDashboard(seriesDays = 30)

        java.time.temporal.ChronoUnit.DAYS.between(seriesFromSlot.captured, seriesToSlot.captured) shouldBe 29L
    }

    test("petDistribution과 signupSeries는 repository의 순서를 보존한다") {
        every { repository.countAllUsers() } returns 0L
        every { repository.countAllPets() } returns 0L
        every { repository.countNewUsersBetween(any(), any()) } returns 0L
        every { repository.countDistinctActiveUsersBetween(any(), any()) } returns 0L
        every { repository.groupPetsByType() } returns
            listOf(
                StatsPetTypeCount("PENGUIN", 100),
                StatsPetTypeCount("MOLE", 50),
            )
        every { repository.getSignupSeries(any(), any()) } returns
            listOf(
                StatsDailyCount("2026-04-22", 0),
                StatsDailyCount("2026-04-23", 5),
                StatsDailyCount("2026-04-24", 0),
            )

        val result = service.getDashboard(seriesDays = 3)

        result.petDistribution.map { it.type } shouldContainExactly listOf("PENGUIN", "MOLE")
        result.signupSeries.map { it.count } shouldContainExactly listOf(0L, 5L, 0L)

        verify(exactly = 1) { repository.groupPetsByType() }
        verify(exactly = 1) { repository.getSignupSeries(any(), any()) }
    }
})
