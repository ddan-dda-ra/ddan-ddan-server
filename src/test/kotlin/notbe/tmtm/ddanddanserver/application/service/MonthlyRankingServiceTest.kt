package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
        totalAttendanceDays: Int = 0,
        userId: ObjectId = ObjectId(),
    ): UserStatEntity =
        UserStatEntity(
            user = User(
                id = userId,
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
            totalAttendanceDays = totalAttendanceDays,
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
        // 순위 상승왕(embed 4,5)은 별도 stub. 기본값은 빈 리스트로 두어 TOP3 embed 테스트에 영향 없게 한다.
        every { userStatRepository.findAllRankingByDateRange(any(), any(), any()) } returns emptyList()
        service = newService(activeProfile = "prod")
    }

    test("세 카테고리 TOP 3 + 순위 상승왕 2개를 조회하여 디스코드에 5개 embed로 발송한다") {
        val caloriesTop = listOf(
            userStat("하드윤", 9172, 2, "MOLE", 2700, totalAttendanceDays = 20),
            userStat("맨정신", 9080, 17, "MOLE", 3900, totalAttendanceDays = 25),
            userStat("성민쓰", 8083, 16, "DOG", 8000, totalAttendanceDays = 18),
        )
        val daysTop = listOf(
            userStat("맨정신", 9080, 17, "MOLE", 3900, totalAttendanceDays = 25),
            userStat("성민쓰", 8083, 16, "DOG", 8000, totalAttendanceDays = 18),
            userStat("아이보리", 4214, 12, "MOLE", 3500, totalAttendanceDays = 10),
        )
        val attendanceTop = listOf(
            userStat("개근왕", 5000, 10, "CAT", 0, totalAttendanceDays = 30),
            userStat("성실맨", 4000, 8, "DOG", 0, totalAttendanceDays = 28),
            userStat("꾸준이", 3000, 5, "PENGUIN", 0, totalAttendanceDays = 21),
        )
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_CALORIES, any(), any(), 3)
        } returns caloriesTop
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_SUCCEEDED_DAYS, any(), any(), 3)
        } returns daysTop
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_ATTENDANCE_DAYS, any(), any(), 3)
        } returns attendanceTop
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
        captured.captured.embeds!!.size shouldBe 5

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

        val attendanceEmbed = captured.captured.embeds!![2]
        attendanceEmbed.title!! shouldContain "📅"
        attendanceEmbed.title!! shouldContain "출석왕 TOP 3"
        attendanceEmbed.color shouldBe 0x5865F2
        attendanceEmbed.description!! shouldContain "🥇 **개근왕** — 30일 출석 · 5,000 cal · 고양이 Lv.1"
        attendanceEmbed.description!! shouldContain "🥈 **성실맨** — 28일 출석"
        attendanceEmbed.description!! shouldContain "🥉 **꾸준이** — 21일 출석"
    }

    test("칼로리/목표달성 순위 상승왕 embed의 제목과 색상이 올바르다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val caloriesRiseEmbed = captured.captured.embeds!![3]
        caloriesRiseEmbed.title!! shouldContain "📈"
        caloriesRiseEmbed.title!! shouldContain "칼로리 순위 상승왕 TOP 3"
        caloriesRiseEmbed.color shouldBe 0xFEE75C

        val daysRiseEmbed = captured.captured.embeds!![4]
        daysRiseEmbed.title!! shouldContain "📈"
        daysRiseEmbed.title!! shouldContain "목표달성 순위 상승왕 TOP 3"
        daysRiseEmbed.color shouldBe 0xEB459E
    }

    test("PROD profile은 마지막 embed footer에 🚀 PROD 접두어를 표시한다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!!.last().footer!!.text shouldContain "🚀 PROD ·"
        captured.captured.embeds!!.last().footer!!.text shouldContain "월간 랭킹 · ddan-ddan-server"
    }

    test("DEV profile은 마지막 embed footer에 🧪 DEV 접두어를 표시한다") {
        service = newService(activeProfile = "dev")
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!!.last().footer!!.text shouldContain "🧪 DEV ·"
    }

    test("그 외 profile은 마지막 embed footer에 💻 접두어를 표시한다") {
        service = newService(activeProfile = "test")
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        captured.captured.embeds!!.last().footer!!.text shouldContain "💻 TEST ·"
    }

    test("footer/timestamp는 마지막 embed에만 부착되고 1~4번 embed에는 없다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val embeds = captured.captured.embeds!!
        embeds.size shouldBe 5
        (0..3).forEach { idx ->
            embeds[idx].footer shouldBe null
            embeds[idx].timestamp shouldBe null
        }
        embeds[4].footer shouldNotBe null
        embeds[4].timestamp shouldNotBe null
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
        captured.captured.embeds!![2].description!! shouldContain "지난 달 데이터가 없습니다"
        captured.captured.embeds!![3].description!! shouldContain "지난 달 순위 상승자가 없습니다"
        captured.captured.embeds!![4].description!! shouldContain "지난 달 순위 상승자가 없습니다"
    }

    test("디스코드 호출이 예외를 던져도 서비스는 예외를 흡수한다") {
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        every { discordHookApi.sendMessage(any()) } throws RuntimeException("webhook down")

        shouldNotThrow<Throwable> {
            service.sendPreviousMonthRanking()
        }
    }

    test("모든 embed 생성이 실패하면 발송을 스킵하고 예외를 흡수한다") {
        // TOP3 조회와 순위 상승왕 조회 모두 예외 → 생성된 embed 0개 → sendMessage 미호출
        every {
            userStatRepository.findRankingByDateRange(any(), any(), any(), any())
        } throws RuntimeException("mongo aggregation failed")
        every {
            userStatRepository.findAllRankingByDateRange(any(), any(), any())
        } throws RuntimeException("mongo aggregation failed")

        shouldNotThrow<Throwable> {
            service.sendPreviousMonthRanking()
        }

        verify(exactly = 0) { discordHookApi.sendMessage(any()) }
    }

    test("순위 상승왕 조회만 실패해도 나머지 3개 embed는 정상 발송된다 (부분 실패 격리)") {
        every {
            userStatRepository.findRankingByDateRange(any(), any(), any(), any())
        } returns listOf(userStat("a", 100, 1, "CAT", 0, totalAttendanceDays = 5))
        // 순위 상승왕(embed 4,5) 조회만 예외
        every {
            userStatRepository.findAllRankingByDateRange(any(), any(), any())
        } throws RuntimeException("rank rise aggregation failed")
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
        // 칼로리/목표달성/출석 3개만 생성됨 (상승왕 2개는 실패로 누락)
        captured.captured.embeds!!.size shouldBe 3
        captured.captured.embeds!![0].footer shouldBe null
        captured.captured.embeds!![1].footer shouldBe null
        captured.captured.embeds!![2].footer shouldNotBe null
        captured.captured.embeds!![2].timestamp shouldNotBe null
    }

    test("한 TOP3 embed 생성이 실패해도 나머지 embed는 정상 발송된다 (부분 실패 격리)") {
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_CALORIES, any(), any(), 3)
        } throws RuntimeException("calories aggregation failed")
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_SUCCEEDED_DAYS, any(), any(), 3)
        } returns emptyList()
        every {
            userStatRepository.findRankingByDateRange(RankingCriteria.TOTAL_ATTENDANCE_DAYS, any(), any(), 3)
        } returns emptyList()
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
        // 칼로리 embed(1개)만 실패 → 5 - 1 = 4개
        captured.captured.embeds!!.size shouldBe 4
        // 첫 embed가 목표달성일 embed로 당겨졌는지(칼로리 누락)
        captured.captured.embeds!![0].title!! shouldContain "목표달성일"
    }

    // --- 순위 상승왕(rankDelta) 계산 로직 검증 ---
    //
    // computeRankRisers는 동일 criteria로 findAllRankingByDateRange를 2회 호출한다:
    //   1) 전전달(month-2) 기간  2) 지난달(month-1) 기간.
    // start 날짜의 월로 두 호출을 구분해 stub한다.

    val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
    val prevMonthStart = today.minusMonths(1).withDayOfMonth(1)
    val prevPrevMonthStart = today.minusMonths(2).withDayOfMonth(1)

    fun stubRankRise(
        criteria: RankingCriteria,
        prevPrevRanking: List<UserStatEntity>,
        prevRanking: List<UserStatEntity>,
    ) {
        every {
            userStatRepository.findAllRankingByDateRange(
                criteria,
                match { it.monthValue == prevPrevMonthStart.monthValue && it.dayOfMonth == 1 },
                any(),
            )
        } returns prevPrevRanking
        every {
            userStatRepository.findAllRankingByDateRange(
                criteria,
                match { it.monthValue == prevMonthStart.monthValue && it.dayOfMonth == 1 },
                any(),
            )
        } returns prevRanking
    }

    test("순위가 오른 유저만(delta>0) 칼로리 순위 상승왕에 노출된다") {
        // month-2: A=1, B=2, C=3  /  month-1: C=1, A=2, B=3
        // → C delta=+2(상승), A delta=-1(하락), B delta=-1(하락) → C만 노출
        val a = ObjectId(); val b = ObjectId(); val c = ObjectId()
        val prevPrev = listOf(
            userStat("A", 300, 0, "CAT", 0, userId = a),
            userStat("B", 200, 0, "CAT", 0, userId = b),
            userStat("C", 100, 0, "CAT", 0, userId = c),
        )
        val prev = listOf(
            userStat("C", 300, 0, "CAT", 0, userId = c),
            userStat("A", 200, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
        )
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        stubRankRise(RankingCriteria.TOTAL_CALORIES, prevPrev, prev)
        stubRankRise(RankingCriteria.TOTAL_SUCCEEDED_DAYS, emptyList(), emptyList())
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val riseDesc = captured.captured.embeds!![3].description!!
        riseDesc shouldContain "🥇 **C** — ▲2계단 (3위 → 1위)"
        riseDesc shouldNotContain "**A**"
        riseDesc shouldNotContain "**B**"
    }

    test("전전달에 없던 신규 유저는 순위 상승왕에서 제외된다") {
        // month-2: A=1, B=2  /  month-1: D(신규)=1, A=2, B=3
        // → A,B 모두 하락, D는 month-2 없음 → 상승자 없음
        val a = ObjectId(); val b = ObjectId(); val d = ObjectId()
        val prevPrev = listOf(
            userStat("A", 200, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
        )
        val prev = listOf(
            userStat("D", 300, 0, "CAT", 0, userId = d),
            userStat("A", 200, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
        )
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        stubRankRise(RankingCriteria.TOTAL_CALORIES, prevPrev, prev)
        stubRankRise(RankingCriteria.TOTAL_SUCCEEDED_DAYS, emptyList(), emptyList())
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val riseDesc = captured.captured.embeds!![3].description!!
        riseDesc shouldContain "지난 달 순위 상승자가 없습니다"
        riseDesc shouldNotContain "**D**"
    }

    test("delta가 동점이면 현재순위(curRank) 작은 유저가 먼저 노출된다") {
        // month-2: A=1, B=2, C=3, D=4  /  month-1: C=1, D=2, A=3, B=4
        // → C delta=+2(curRank=1), D delta=+2(curRank=2), A delta=-2, B delta=-2
        //   delta 동점(C,D=+2) → curRank ASC → C가 먼저(1위), D가 다음
        val a = ObjectId(); val b = ObjectId(); val c = ObjectId(); val d = ObjectId()
        val prevPrev = listOf(
            userStat("A", 400, 0, "CAT", 0, userId = a),
            userStat("B", 300, 0, "CAT", 0, userId = b),
            userStat("C", 200, 0, "CAT", 0, userId = c),
            userStat("D", 100, 0, "CAT", 0, userId = d),
        )
        val prev = listOf(
            userStat("C", 400, 0, "CAT", 0, userId = c),
            userStat("D", 300, 0, "CAT", 0, userId = d),
            userStat("A", 200, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
        )
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        stubRankRise(RankingCriteria.TOTAL_CALORIES, prevPrev, prev)
        stubRankRise(RankingCriteria.TOTAL_SUCCEEDED_DAYS, emptyList(), emptyList())
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val riseDesc = captured.captured.embeds!![3].description!!
        riseDesc shouldContain "🥇 **C** — ▲2계단 (3위 → 1위)"
        riseDesc shouldContain "🥈 **D** — ▲2계단 (4위 → 2위)"
        // C가 D보다 앞에 위치
        riseDesc.indexOf("**C**") shouldBeLessThan riseDesc.indexOf("**D**")
    }

    test("동점자는 같은 순위로 계산되어 tie-break 순서 변경만으로 순위 상승왕에 노출되지 않는다") {
        val a = ObjectId(); val b = ObjectId(); val c = ObjectId()
        val prevPrev = listOf(
            userStat("A", 100, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
            userStat("C", 100, 0, "CAT", 0, userId = c),
        )
        val prev = listOf(
            userStat("C", 100, 0, "CAT", 0, userId = c),
            userStat("A", 100, 0, "CAT", 0, userId = a),
            userStat("B", 100, 0, "CAT", 0, userId = b),
        )
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        stubRankRise(RankingCriteria.TOTAL_CALORIES, prevPrev, prev)
        stubRankRise(RankingCriteria.TOTAL_SUCCEEDED_DAYS, emptyList(), emptyList())
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val riseDesc = captured.captured.embeds!![3].description!!
        riseDesc shouldContain "지난 달 순위 상승자가 없습니다"
        riseDesc shouldNotContain "**C**"
    }

    test("순위 상승자가 TOP3을 넘으면 상위 3명만 노출된다") {
        // month-2: 꼴찌권(E=5,D=4,C=3,B=2,A=1)
        // month-1: E=1,D=2,C=3,B=4,A=5  → E +4, D +2, C 0, B -2, A -4
        //   상승자 E(+4), D(+2) 둘뿐 → 그대로. TOP3 컷 확인용으로 5명 중 상승 3명 이상 시나리오 구성
        // month-2: A=1,B=2,C=3,D=4,E=5  /  month-1: E=1,D=2,C=3,B=4,A=5
        // → E +4, D +2, C 0, B -2, A -4 → 상승 E,D (2명)
        // TOP3 컷 확인은 상승자 4명 이상 시나리오로:
        // month-2: A=1,B=2,C=3,D=4,E=5  /  month-1: B=1,C=2,D=3,E=4,A=5
        // → B +1, C +1, D +1, E +1, A -4 → 상승자 4명(B,C,D,E) → TOP3컷 → 3명만
        val a = ObjectId(); val b = ObjectId(); val c = ObjectId(); val d = ObjectId(); val e = ObjectId()
        val prevPrev = listOf(
            userStat("A", 500, 0, "CAT", 0, userId = a),
            userStat("B", 400, 0, "CAT", 0, userId = b),
            userStat("C", 300, 0, "CAT", 0, userId = c),
            userStat("D", 200, 0, "CAT", 0, userId = d),
            userStat("E", 100, 0, "CAT", 0, userId = e),
        )
        val prev = listOf(
            userStat("B", 500, 0, "CAT", 0, userId = b),
            userStat("C", 400, 0, "CAT", 0, userId = c),
            userStat("D", 300, 0, "CAT", 0, userId = d),
            userStat("E", 200, 0, "CAT", 0, userId = e),
            userStat("A", 100, 0, "CAT", 0, userId = a),
        )
        every { userStatRepository.findRankingByDateRange(any(), any(), any(), any()) } returns emptyList()
        stubRankRise(RankingCriteria.TOTAL_CALORIES, prevPrev, prev)
        stubRankRise(RankingCriteria.TOTAL_SUCCEEDED_DAYS, emptyList(), emptyList())
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        service.sendPreviousMonthRanking()

        val riseDesc = captured.captured.embeds!![3].description!!
        // 4명 상승했으나 TOP3만 → 메달 3개, 4번째(🥇🥈🥉 외) 없음
        riseDesc.lines() shouldHaveSize 3
        // delta 동점(+1) → curRank ASC: B(1)→C(2)→D(3)이 노출, E(4)는 컷
        riseDesc shouldContain "🥇 **B**"
        riseDesc shouldContain "🥈 **C**"
        riseDesc shouldContain "🥉 **D**"
        riseDesc shouldNotContain "**E**"
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
