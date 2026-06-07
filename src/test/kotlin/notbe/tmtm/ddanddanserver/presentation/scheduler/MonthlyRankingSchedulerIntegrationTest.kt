package notbe.tmtm.ddanddanserver.presentation.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.MonthlyRankingService
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 회귀 방지 통합 테스트.
 *
 * 단위 테스트(MonthlyRankingServiceTest)는 service를 직접 호출해
 * scheduler→service wiring과 Spring 빈 등록을 검증할 수 없다.
 *
 * 본 테스트는 Spring TestContext로 Scheduler + Service + 의존 mock을 등록하고
 * scheduler.sendPreviousMonthRanking()을 호출하여 전체 흐름이 동작하는지 검증한다.
 *
 * @Scheduled의 cron 트리거 자체(매월 1일 00:05 KST)는 시간 조작이 필요해 통합 테스트로
 * 검증이 비현실적이므로, production 운영으로 검증.
 */
@SpringJUnitConfig(
    classes = [
        MonthlyRankingScheduler::class,
        MonthlyRankingService::class,
        MonthlyRankingSchedulerIntegrationTest.TestConfig::class,
    ],
)
@TestPropertySource(properties = ["spring.profiles.active=test"])
class MonthlyRankingSchedulerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun userStatRepository(): UserStatRepository = mockk(relaxed = true)

        @Bean
        fun discordHookApi(): DiscordHookApi = mockk(relaxed = true)

        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var scheduler: MonthlyRankingScheduler

    @Autowired
    lateinit var userStatRepository: UserStatRepository

    @Autowired
    lateinit var discordHookApi: DiscordHookApi

    @Test
    fun `Spring 컨텍스트에서 scheduler 트리거 시 service-repository-discord 호출 흐름이 동작한다`() {
        every {
            userStatRepository.findRankingByDateRange(any(), any(), any(), any())
        } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        scheduler.sendPreviousMonthRanking()

        verify(exactly = 1) {
            userStatRepository.findRankingByDateRange(
                RankingCriteria.TOTAL_CALORIES,
                any(),
                any(),
                3,
            )
        }
        verify(exactly = 1) {
            userStatRepository.findRankingByDateRange(
                RankingCriteria.TOTAL_SUCCEEDED_DAYS,
                any(),
                any(),
                3,
            )
        }
        verify(exactly = 1) {
            userStatRepository.findRankingByDateRange(
                RankingCriteria.TOTAL_ATTENDANCE_DAYS,
                any(),
                any(),
                3,
            )
        }
        verify(exactly = 1) { discordHookApi.sendMessage(any()) }

        // wiring + phase 주입까지 동작하는지 확인 (embed 5개, footer는 마지막 embed)
        assertEquals(5, captured.captured.embeds!!.size)
        assertTrue(captured.captured.embeds!!.last().footer!!.text.contains("TEST"))
    }
}
