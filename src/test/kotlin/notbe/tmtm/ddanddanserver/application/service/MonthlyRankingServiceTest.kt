package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId

class MonthlyRankingServiceTest : FunSpec({
    lateinit var userStatRepository: UserStatRepository
    lateinit var discordHookApi: DiscordHookApi
    lateinit var petCatalogService: PetCatalogService
    lateinit var service: MonthlyRankingService

    fun newService(activeProfile: String): MonthlyRankingService =
        MonthlyRankingService(
            userStatRepository = userStatRepository,
            discordHookApi = discordHookApi,
            petCatalogService = petCatalogService,
            activeProfile = activeProfile,
        )

    fun userStat(
        name: String,
        totalCalories: Int,
        totalSucceededDays: Int,
        petType: String,
        petExp: Int,
    ): UserStatEntity =
        UserStatEntity(
            user = User(
                id = ObjectId(),
                deviceToken = null,
                name = name,
                setting = UserSetting(),
            ),
            mainPet = Pet(
                id = ObjectId(),
                type = petType,
                ownerUserId = ObjectId(),
                exp = petExp,
            ),
            totalCalories = totalCalories,
            totalSucceededDays = totalSucceededDays,
        )

    beforeEach {
        userStatRepository = mockk()
        discordHookApi = mockk(relaxed = true)
        petCatalogService = mockk()
        every { petCatalogService.getName("CAT") } returns "고양이"
        every { petCatalogService.getName("HAMSTER") } returns "햄스터"
        every { petCatalogService.getName("PENGUIN") } returns "펭귄"
        every { petCatalogService.getName("DOG") } returns "강아지"
        every { petCatalogService.getName("MOLE") } returns "두더지"
        service = newService(activeProfile = "prod")
    }

    test("두 카테고리 TOP 3을 조회하여 디스코드에 2개 embed로 발송한다") {
        val caloriesTop = listOf(
            userStat("하드윤", 9172, 2, "MOLE", 2700),
            userStat("맨정신", 9080, 17, "MOLE", 3900),
            userStat("성민쓰", 8083, 16, "DOG", 8000),
        )
        val daysTop = listOf(
            userStat("맨정신", 9080, 17, "MOLE", 3900),
            userStat("성민쓰", 8083, 16, "DOG", 8000),
            userStat("아이보리", 4214, 12, "MOLE", 3500),
        )
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_CALORIES, any(), any(), 3)
        } returns caloriesTop
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_SUCCEEDED_DAYS, any(), any(), 3)
        } returns daysTop
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
        captured.captured.embeds!!.size shouldBe 2

        val caloriesEmbed = captured.captured.embeds!![0]
        caloriesEmbed.title!! shouldContain "🔥"
        caloriesEmbed.title!! shouldContain "칼로리 TOP 3"
        caloriesEmbed.color shouldBe 0xFF7F00
        caloriesEmbed.description!! shouldContain "🥇 **하드윤** — 9,172 cal · 달성 2일 · 두더지 Lv.4"
        caloriesEmbed.description!! shouldContain "🥈 **맨정신**"
        caloriesEmbed.description!! shouldContain "🥉 **성민쓰**"

        val daysEmbed = captured.captured.embeds!![1]
        daysEmbed.title!! shouldContain "🎯"
        daysEmbed.title!! shouldContain "목표달성일 TOP 3"
        daysEmbed.color shouldBe 0x57F287
        daysEmbed.description!! shouldContain "🥇 **맨정신** — 17일 · 9,080 cal · 두더지 Lv.4"
        daysEmbed.description!! shouldContain "🥉 **아이보리** — 12일 · 4,214 cal · 두더지 Lv.4"
    }

    test("PROD profile은 footer에 🚀 PROD 접두어를 표시한다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!![1].footer!!.text shouldContain "🚀 PROD ·"
        captured.captured.embeds!![1].footer!!.text shouldContain "월간 랭킹 · ddan-ddan-server"
    }

    test("DEV profile은 footer에 🧪 DEV 접두어를 표시한다") {
        service = newService(activeProfile = "dev")
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!![1].footer!!.text shouldContain "🧪 DEV ·"
    }

    test("그 외 profile은 footer에 💻 접두어를 표시한다") {
        service = newService(activeProfile = "test")
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!![1].footer!!.text shouldContain "💻 TEST ·"
    }

    test("모든 펫 타입은 카탈로그 표시명으로 표기된다") {
        val sample = listOf(
            userStat("a", 100, 1, "CAT", 0),
            userStat("b", 100, 1, "HAMSTER", 0),
            userStat("c", 100, 1, "PENGUIN", 0),
        )
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_CALORIES, any(), any(), 3)
        } returns sample
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_SUCCEEDED_DAYS, any(), any(), 3)
        } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val desc = captured.captured.embeds!![0].description!!
        desc shouldContain "고양이"
        desc shouldContain "햄스터"
        desc shouldContain "펭귄"
    }

    test("카탈로그에 없는 키는 키 자체를 표시한다 (fallback)") {
        every { petCatalogService.getName("UNKNOWN") } returns null
        val sample = listOf(userStat("x", 100, 1, "UNKNOWN", 0))
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_CALORIES, any(), any(), 3)
        } returns sample
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_SUCCEEDED_DAYS, any(), any(), 3)
        } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!![0].description!! shouldContain "UNKNOWN"
    }

    test("데이터가 없으면 description에 안내 문구가 들어간다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!![0].description!! shouldContain "지난 달 데이터가 없습니다"
        captured.captured.embeds!![1].description!! shouldContain "지난 달 데이터가 없습니다"
    }

    test("디스코드 호출이 예외를 던져도 서비스는 예외를 흡수한다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        every { discordHookApi.sendMessage(any()) } throws RuntimeException("webhook down")

        shouldNotThrow<Throwable> {
            service.sendPreviousMonthRanking()
        }
    }

    test("Repository 호출이 예외를 던져도 서비스는 예외를 흡수한다") {
        every {
            userStatRepository.findRankingByDateRange(any(), any(), any(), any())
        } throws RuntimeException("mongo aggregation failed")

        shouldNotThrow<Throwable> {
            service.sendPreviousMonthRanking()
        }

        verify(exactly = 0) { discordHookApi.sendMessage(any()) }
    }

    test("두 카테고리에 대해 직전 달 1일~말일을 조회한다") {
        val startSlot = slot<java.time.LocalDate>()
        val endSlot = slot<java.time.LocalDate>()
        every {
            userStatRepository.findRankingByDateRange(any(), capture(startSlot), capture(endSlot), 3)
        } returns emptyList()

        service.sendPreviousMonthRanking()

        startSlot.captured.dayOfMonth shouldBe 1
        endSlot.captured.dayOfMonth shouldBe startSlot.captured.lengthOfMonth()
        startSlot.captured.month shouldBe endSlot.captured.month
        startSlot.captured.year shouldBe endSlot.captured.year
        startSlot.captured.month shouldNotBe java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).month
    }
})
