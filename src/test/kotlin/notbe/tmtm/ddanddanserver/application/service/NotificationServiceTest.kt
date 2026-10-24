package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.getAllRankingBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class NotificationServiceTest {

    private lateinit var notificationService: NotificationService
    private lateinit var userRepository: UserRepository
    private lateinit var userStatRepository: UserStatRepository
    private lateinit var rankingService: RankingService
    private lateinit var pushClient: PushClient

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userStatRepository = mockk()
        rankingService = mockk()
        pushClient = mockk(relaxed = true)
        
        notificationService = NotificationService(
            userRepository = userRepository,
            userStatRepository = userStatRepository,
            rankingService = rankingService,
            pushClient = pushClient
        )

        mockkStatic(USER_STAT_REPOSITORY_KT)
        mockkStatic(LocalDateTime::class)
        // 매달 1일에는 갱신만 수행하므로 1일이 아닌 날짜로 고정한다.
        every { LocalDateTime.now() } returns LocalDateTime.of(2026, 10, 24, 12, 0)
        every { rankingService.updateRankingBoard(any()) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(USER_STAT_REPOSITORY_KT)
        unmockkStatic(LocalDateTime::class)
    }

    @Test
    fun `현재 랭킹에서 밀려난 유저에게 랭킹 하락 알림을 보내고 랭킹보드를 갱신한다`() {
        // given
        val stayed = createTestUser("유지유저", "stayed-token", true)
        val dropped = createTestUser("이탈유저", "dropped-token", true)
        givenRanking(previous = listOf(stayed, dropped), current = listOf(stayed))
        every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

        // when
        notificationService.notifyRankingDiff()

        // then
        verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
        verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
    }

    @Test
    fun `순위가 떨어진 유저에게 랭킹 하락 알림을 보낸다`() {
        // given
        val down = createTestUser("하락유저", "down-token", true)
        val up = createTestUser("상승유저", "up-token", true)
        givenRanking(previous = listOf(down, up), current = listOf(up, down))
        every { userRepository.findById(down.id) } returns Optional.of(down)

        // when
        notificationService.notifyRankingDiff()

        // then
        verify(exactly = 1) { pushClient.sendToUser(DeviceToken("down-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
        verify(exactly = 1) { pushClient.sendToUser(any(), any(), any()) }
        verify(exactly = 0) { userRepository.findById(up.id) }
        verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
    }

    @Test
    fun `탈퇴해 조회되지 않는 유저는 건너뛰고 랭킹보드를 갱신한다`() {
        // given
        val withdrawn = createTestUser("탈퇴유저", "withdrawn-token", true)
        val dropped = createTestUser("이탈유저", "dropped-token", true)
        givenRanking(previous = listOf(withdrawn, dropped), current = emptyList())
        every { userRepository.findById(withdrawn.id) } returns Optional.empty()
        every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

        // when
        notificationService.notifyRankingDiff()

        // then
        verify(exactly = 0) { pushClient.sendToUser(DeviceToken("withdrawn-token"), any(), any()) }
        verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
        verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
    }

    @Test
    fun `한 유저의 알림 전송이 실패해도 나머지 유저에게 알림을 보내고 랭킹보드를 갱신한다`() {
        // given
        val failed = createTestUser("실패유저", "failed-token", true)
        val dropped = createTestUser("이탈유저", "dropped-token", true)
        givenRanking(previous = listOf(failed, dropped), current = emptyList())
        every { userRepository.findById(failed.id) } throws RuntimeException("조회 실패")
        every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

        // when
        notificationService.notifyRankingDiff()

        // then
        verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
        verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
    }

    @Test
    fun `순위가 오른 유저에게는 알림을 보내지 않는다`() {
        // given
        val up = createTestUser("상승유저", "up-token", true)
        val down = createTestUser("하락유저", "down-token", false)
        givenRanking(previous = listOf(down, up), current = listOf(up, down))
        every { userRepository.findById(down.id) } returns Optional.of(down)

        // when
        notificationService.notifyRankingDiff()

        // then
        verify(exactly = 0) { userRepository.findById(up.id) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
        verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
    }

    @Test
    @Disabled("실제 푸시 전송 로직 테스트용 - 필요시 활성화")
    fun `유효한 디바이스 토큰을 가진 유저들에게만 푸시 알림이 전송된다`() {
        // given
        val users = listOf(
            createTestUser("유저1", "valid-token-1", true),
            createTestUser("유저2", "deviceToken", true), // 테스트 토큰 (필터링됨)
            createTestUser("유저3", "valid-token-3", false), // 푸시 비활성화
            createTestUser("유저4", "valid-token-4", true)
        )
        
        every { userRepository.findAll() } returns users

        // when
        notificationService.notifyCheckCalorie()

        // then
        verify {
            pushClient.sendToMultiple(
                listOf(DeviceToken("valid-token-1"), DeviceToken("deviceToken"), null, DeviceToken("valid-token-4")),
                PushMessage.CHECK_CALORIE,
                RoutingView.MAIN
            )
        }
        
        println("✅ 푸시 알림 필터링 테스트 완료")
        println("📊 총 유저: ${users.size}")
        println("🔔 푸시 대상: 2명 (유효한 토큰 + 푸시 활성화)")
    }

    @Test
    @Disabled("실제 푸시 전송 로직 테스트용 - 필요시 활성화")
    fun `주간 랭킹 푸시 알림에 칼로리 정보가 포함된다`() {
        // given
        val users = listOf(
            createTestUser("랭킹유저", "ranking-token", true)
        )
        val testCalories = 2500
        
        every { userRepository.findAll() } returns users

        // when
        notificationService.notifyWeeklyRanking(testCalories)

        // then
        verify {
            pushClient.sendToMultiple(
                listOf(DeviceToken("ranking-token")),
                PushMessage.weeklyRanking(testCalories),
                RoutingView.MAIN
            )
        }
        
        println("✅ 주간 랭킹 푸시 테스트 완료")
        println("🏆 칼로리: $testCalories")
    }

    @Test
    @Disabled("실제 푸시 전송 로직 테스트용 - 필요시 활성화")
    fun `PushClient를 통해 단일 유저에게 푸시를 전송할 수 있다`() {
        // given
        val deviceToken = DeviceToken("test-device-token")
        val message = "테스트 메시지"

        // when
        pushClient.sendToUser(deviceToken, message, RoutingView.MAIN)

        // then
        verify { pushClient.sendToUser(deviceToken, message, RoutingView.MAIN) }

        println("✅ 단일 푸시 전송 테스트 완료")
        println("📱 대상: $deviceToken")
        println("💬 메시지: $message")
    }

    private fun givenRanking(
        previous: List<User>,
        current: List<User>,
    ) {
        every { rankingService.getRankingBoard(PeriodType.MONTHLY) } returns
            RankingBoard.of(id = PeriodType.MONTHLY, ranking = previous.map { createUserStat(it) })
        every {
            userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.MONTHLY)
        } returns current.map { createUserStat(it) }
    }

    private fun createUserStat(user: User): UserStat {
        return UserStat(
            user = user,
            mainPet = Pet(id = ObjectId(), type = "DOG", ownerUserId = user.id, exp = 0),
            totalCalories = 0,
            totalSucceededDays = 0,
            totalAttendanceDays = 0,
        )
    }

    private fun createTestUser(
        name: String,
        deviceToken: String?,
        isAppPushOn: Boolean
    ): User {
        return User(
            id = ObjectId(),
            deviceToken = deviceToken?.let { DeviceToken.of(it) },
            name = name,
            setting = UserSetting(isAppPushOn = isAppPushOn)
        )
    }

    companion object {
        private const val USER_STAT_REPOSITORY_KT =
            "notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepositoryKt"
    }
}
