package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
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

class NotificationServiceTest : FunSpec({
    lateinit var userRepository: UserRepository
    lateinit var userStatRepository: UserStatRepository
    lateinit var rankingService: RankingService
    lateinit var pushClient: PushClient
    lateinit var notificationService: NotificationService

    beforeEach {
        userRepository = mockk()
        userStatRepository = mockk()
        rankingService = mockk()
        pushClient = mockk(relaxed = true)
        notificationService = NotificationService(
            userRepository = userRepository,
            userStatRepository = userStatRepository,
            rankingService = rankingService,
            pushClient = pushClient,
        )
    }

    fun createTestUser(name: String, deviceToken: String?, isAppPushOn: Boolean): User =
        User(
            id = ObjectId(),
            deviceToken = DeviceToken.of(deviceToken),
            name = name,
            setting = UserSetting(isAppPushOn = isAppPushOn),
        )

    test("칼로리 체크 알림은 푸시가 켜져 있고 유효한 토큰을 가진 유저에게만 전송된다") {
        every { userRepository.findAll() } returns listOf(
            createTestUser("유저1", "valid-token-1", true),
            createTestUser("유저2", "deviceToken", true), // 기본 토큰 (제외)
            createTestUser("유저3", "valid-token-3", false), // 푸시 비활성화 (제외)
            createTestUser("유저4", null, true), // 토큰 없음 (제외)
            createTestUser("유저5", "valid-token-5", true),
        )

        notificationService.notifyCheckCalorie()

        verify(exactly = 1) {
            pushClient.sendToMultiple(
                listOf(DeviceToken("valid-token-1"), DeviceToken("valid-token-5")),
                PushMessage.CHECK_CALORIE,
                RoutingView.MAIN,
            )
        }
    }

    test("주간 랭킹 알림에 칼로리 정보가 포함되어 전송된다") {
        val totalCalories = 2500
        every { userRepository.findAll() } returns listOf(
            createTestUser("랭킹유저", "ranking-token", true),
        )

        notificationService.notifyWeeklyRanking(totalCalories)

        verify(exactly = 1) {
            pushClient.sendToMultiple(
                listOf(DeviceToken("ranking-token")),
                PushMessage.weeklyRanking(totalCalories),
                RoutingView.MAIN,
            )
        }
    }

    test("푸시를 받을 수 있는 유저가 없으면 일괄 알림을 전송하지 않는다") {
        every { userRepository.findAll() } returns listOf(
            createTestUser("유저1", "valid-token-1", false),
            createTestUser("유저2", "deviceToken", true),
        )

        notificationService.notifyCheckCalorie()
        notificationService.notifyWeeklyRanking(1000)

        verify(exactly = 0) { pushClient.sendToMultiple(any(), any(), any()) }
    }
})
