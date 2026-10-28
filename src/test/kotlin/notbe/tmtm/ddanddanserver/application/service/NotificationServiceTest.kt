package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
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
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.getAllRankingBy
import org.bson.types.ObjectId
import java.time.Instant
import java.time.LocalDateTime
import java.util.Optional

private const val USER_STAT_REPOSITORY_KT =
    "notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepositoryKt"

class NotificationServiceTest : FunSpec({
    lateinit var userRepository: UserRepository
    lateinit var userStatRepository: UserStatRepository
    lateinit var petCatalogRepository: PetCatalogRepository
    lateinit var rankingService: RankingService
    lateinit var pushClient: PushClient
    lateinit var notificationService: NotificationService

    beforeEach {
        userRepository = mockk()
        userStatRepository = mockk()
        petCatalogRepository = mockk()
        rankingService = mockk()
        pushClient = mockk(relaxed = true)
        notificationService = NotificationService(
            userRepository = userRepository,
            userStatRepository = userStatRepository,
            petCatalogRepository = petCatalogRepository,
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

    context("notifyNewPetArrived") {
        val until = Instant.parse("2026-10-28T05:00:00Z") // 2026-10-28 14:00 KST
        val from = Instant.parse("2026-10-27T05:00:00Z")

        fun givenNewPetActivated(exists: Boolean) {
            every {
                petCatalogRepository.existsActivatedBetween(from, until)
            } returns exists
        }

        test("직전 하루 동안 공개된 펫이 있으면 푸시가 켜진 유저에게 새 펫 알림을 한 번 보낸다") {
            givenNewPetActivated(true)
            every { userRepository.findAll() } returns listOf(
                createTestUser("유저1", "valid-token-1", true),
                createTestUser("유저2", "valid-token-2", false), // 푸시 비활성화 (제외)
            )

            notificationService.notifyNewPetArrived(until)

            verify(exactly = 1) {
                pushClient.sendToMultiple(
                    listOf(DeviceToken("valid-token-1")),
                    PushMessage.NEW_PET_ARRIVED,
                    RoutingView.MAIN,
                )
            }
        }

        test("직전 하루 동안 공개된 펫이 없으면 알림을 보내지 않는다") {
            givenNewPetActivated(false)

            notificationService.notifyNewPetArrived(until)

            verify(exactly = 0) { userRepository.findAll() }
            verify(exactly = 0) { pushClient.sendToMultiple(any(), any(), any()) }
        }
    }

    context("notifyRankingDiff") {
        fun createUserStat(user: User): UserStat =
            UserStat(
                user = user,
                mainPet = Pet(id = ObjectId(), type = "DOG", ownerUserId = user.id, exp = 0),
                totalCalories = 0,
                totalSucceededDays = 0,
                totalAttendanceDays = 0,
            )

        fun givenRanking(previous: List<User>, current: List<User>) {
            every { rankingService.getRankingBoard(PeriodType.MONTHLY) } returns
                RankingBoard.of(id = PeriodType.MONTHLY, ranking = previous.map { createUserStat(it) })
            every {
                userStatRepository.getAllRankingBy(RankingCriteria.TOTAL_CALORIES, PeriodType.MONTHLY)
            } returns current.map { createUserStat(it) }
        }

        beforeEach {
            mockkStatic(USER_STAT_REPOSITORY_KT)
            mockkStatic(LocalDateTime::class)
            // 매달 1일에는 갱신만 수행하므로 1일이 아닌 날짜로 고정한다.
            every { LocalDateTime.now() } returns LocalDateTime.of(2026, 10, 24, 12, 0)
            every { rankingService.updateRankingBoard(any()) } answers { firstArg() }
        }

        afterEach {
            unmockkStatic(USER_STAT_REPOSITORY_KT)
            unmockkStatic(LocalDateTime::class)
        }

        test("현재 랭킹에서 밀려난 유저에게 랭킹 하락 알림을 보내고 랭킹보드를 갱신한다") {
            val stayed = createTestUser("유지유저", "stayed-token", true)
            val dropped = createTestUser("이탈유저", "dropped-token", true)
            givenRanking(previous = listOf(stayed, dropped), current = listOf(stayed))
            every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

            notificationService.notifyRankingDiff()

            verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
            verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
        }

        test("순위가 떨어진 유저에게 랭킹 하락 알림을 보낸다") {
            val down = createTestUser("하락유저", "down-token", true)
            val up = createTestUser("상승유저", "up-token", true)
            givenRanking(previous = listOf(down, up), current = listOf(up, down))
            every { userRepository.findById(down.id) } returns Optional.of(down)

            notificationService.notifyRankingDiff()

            verify(exactly = 1) { pushClient.sendToUser(DeviceToken("down-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
            verify(exactly = 1) { pushClient.sendToUser(any(), any(), any()) }
            verify(exactly = 0) { userRepository.findById(up.id) }
            verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
        }

        test("탈퇴해 조회되지 않는 유저는 건너뛰고 랭킹보드를 갱신한다") {
            val withdrawn = createTestUser("탈퇴유저", "withdrawn-token", true)
            val dropped = createTestUser("이탈유저", "dropped-token", true)
            givenRanking(previous = listOf(withdrawn, dropped), current = emptyList())
            every { userRepository.findById(withdrawn.id) } returns Optional.empty()
            every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

            notificationService.notifyRankingDiff()

            verify(exactly = 0) { pushClient.sendToUser(DeviceToken("withdrawn-token"), any(), any()) }
            verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
            verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
        }

        test("한 유저의 알림 전송이 실패해도 나머지 유저에게 알림을 보내고 랭킹보드를 갱신한다") {
            val failed = createTestUser("실패유저", "failed-token", true)
            val dropped = createTestUser("이탈유저", "dropped-token", true)
            givenRanking(previous = listOf(failed, dropped), current = emptyList())
            every { userRepository.findById(failed.id) } throws RuntimeException("조회 실패")
            every { userRepository.findById(dropped.id) } returns Optional.of(dropped)

            notificationService.notifyRankingDiff()

            verify(exactly = 1) { pushClient.sendToUser(DeviceToken("dropped-token"), PushMessage.RANKING_DOWN, RoutingView.MAIN) }
            verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
        }

        test("순위가 오른 유저에게는 알림을 보내지 않는다") {
            val up = createTestUser("상승유저", "up-token", true)
            val down = createTestUser("하락유저", "down-token", false)
            givenRanking(previous = listOf(down, up), current = listOf(up, down))
            every { userRepository.findById(down.id) } returns Optional.of(down)

            notificationService.notifyRankingDiff()

            verify(exactly = 0) { userRepository.findById(up.id) }
            verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
            verify(exactly = 1) { rankingService.updateRankingBoard(any()) }
        }
    }
})
