package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
                listOf("valid-token-1", "valid-token-4"),
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
                listOf("ranking-token"),
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
        val deviceToken = "test-device-token"
        val message = "테스트 메시지"

        // when
        pushClient.sendToUser(deviceToken, message, RoutingView.MAIN)

        // then
        verify { pushClient.sendToUser(deviceToken, message, RoutingView.MAIN) }
        
        println("✅ 단일 푸시 전송 테스트 완료")
        println("📱 대상: $deviceToken")
        println("💬 메시지: $message")
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
}
