package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.deleteAllBy
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataAccessResourceFailureException
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var dailyInfoRepository: DailyInfoRepository
    private lateinit var petRepository: PetRepository
    private lateinit var userService: UserService
    private lateinit var inviteCodeRepository: InviteCodeRepository
    private lateinit var friendshipRepository: FriendshipRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        authRepository = mockk()
        dailyInfoRepository = mockk()
        petRepository = mockk()
        inviteCodeRepository = mockk()
        friendshipRepository = mockk()
        userService =
            UserService(
                userRepository,
                authRepository,
                dailyInfoRepository,
                petRepository,
                inviteCodeRepository,
                friendshipRepository,
            )
    }

    @Test
    fun `사용자 조회가 성공해야 한다`() {
        // given
        val userId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(userId) } returns user

        // when
        val result = userService.getByIdOrThrow(userId)

        // then
        assertNotNull(result)
        assertEquals(user, result)
    }

    @Test
    fun `모든 사용자 조회가 성공해야 한다`() {
        // given
        val users = listOf(mockk<User>(), mockk<User>(), mockk<User>())
        every { userRepository.findAll() } returns users

        // when
        val result = userService.getAll()

        // then
        assertEquals(3, result.size)
        assertEquals(users, result)
    }

    @Test
    fun `사용자 정보 업데이트가 성공해야 한다`() {
        // given
        val userId = ObjectId()
        val user = mockk<User>(relaxed = true)
        val updatedUser = mockk<User>()
        val newName = "새로운 이름"
        val newPurposeCalorie = 2000

        every { user.name } returns newName
        every { user.purposeCalorie } returns newPurposeCalorie
        every { userRepository.findByIdOrThrow(userId) } returns user
        every { userRepository.updateProfile(userId, newName, newPurposeCalorie) } returns updatedUser

        // when
        val result = userService.updateInfo(userId, newName, newPurposeCalorie)

        // then
        verify { user.updateInfo(newName, newPurposeCalorie) }
        verify { userRepository.updateProfile(userId, newName, newPurposeCalorie) }
        verify(exactly = 0) { userRepository.save(any()) }
        assertEquals(updatedUser, result)
    }

    @Test
    fun `사용자 설정 업데이트가 성공해야 한다`() {
        // given
        val userId = ObjectId()
        val oldSetting = UserSetting(isAppPushOn = false)
        val newSetting = UserSetting(isAppPushOn = true)
        val user =
            mockk<User> {
                every { setting } returns oldSetting
            }
        val updatedUser =
            mockk<User> {
                every { setting } returns newSetting
            }

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { userRepository.updateSetting(userId, newSetting) } returns updatedUser

        // when
        val result = userService.updateSetting(userId, true)

        // then
        verify { userRepository.updateSetting(userId, newSetting) }
        verify(exactly = 0) { userRepository.save(any()) }
        assertEquals(newSetting, result)
    }

    @Test
    fun `사용자 탈퇴가 성공해야 한다`() {
        // given
        val userId = ObjectId()

        every { authRepository.deleteAllByUserId(userId) } returns Unit
        every { petRepository.deleteAllByOwnerUserId(userId) } returns Unit
        every { dailyInfoRepository.deleteAllByUserId(userId) } returns Unit
        every { inviteCodeRepository.deleteAllByInviterId(userId) } returns Unit
        every { friendshipRepository.deleteAllBy(userId) } returns Unit
        every { userRepository.deleteById(userId) } returns Unit

        // when
        userService.withdraw(userId)

        // then
        verify { authRepository.deleteAllByUserId(userId) }
        verify { petRepository.deleteAllByOwnerUserId(userId) }
        verify { dailyInfoRepository.deleteAllByUserId(userId) }
        verify { inviteCodeRepository.deleteAllByInviterId(userId) }
        verify { friendshipRepository.deleteAllBy(userId) }

        verify { userRepository.deleteById(userId) }
    }

    @Test
    fun `어제 목표를 달성한 유저를 제외하고 연속 달성을 초기화해야 한다`() {
        // given
        val achievedUserIds = listOf(ObjectId(), ObjectId())
        every { userRepository.resetPurposeStrictExcept(achievedUserIds) } returns 3L

        // when
        val result = userService.resetPurposeStrictExcept(achievedUserIds)

        // then
        assertEquals(3L, result)
    }

    @Nested
    inner class UpdateCalorieAndRewardFood {
        private val userId = ObjectId()
        private val today = LocalDate.now()
        private val dailyInfo = DailyInfo.create(userId, "유저", "DOG", today)

        @BeforeEach
        fun setUpDailyInfo() {
            every { userRepository.findByIdOrThrow(userId) } returns user(purposeCalorie = 300)
            every { dailyInfoRepository.findByUserIdAndDate(userId, today) } returns dailyInfo
            every { dailyInfoRepository.findById(dailyInfo.id) } returns Optional.of(dailyInfo)
        }

        @Test
        fun `갱신 전 칼로리 기준으로 100kcal마다 먹이를 지급해야 한다`() {
            // given
            val rewardedUser = user(foodQuantity = 2)
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 250) } returns 50
            every { userRepository.rewardDailyCalorie(userId, 2, false, any()) } returns rewardedUser

            // when
            val result = userService.updateCalorieAndRewardFood(userId, 250, today)

            // then
            assertEquals(2, result.rewardedFoodQuantity)
            assertEquals(0, result.rewardedToyQuantity)
            assertEquals(rewardedUser, result.user)
            verify(exactly = 0) { dailyInfoRepository.markPurposeAchieved(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
            verify(exactly = 0) { dailyInfoRepository.save(any()) }
        }

        @Test
        fun `동시 요청이 먼저 칼로리를 올렸으면 그 값 기준으로 계산해 보상을 중복 지급하지 않아야 한다`() {
            // given: 다른 요청이 이미 250으로 올려 둔 상태에서 같은 250이 다시 들어온다
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 250) } returns 250
            every { userRepository.rewardDailyCalorie(userId, 0, false, any()) } returns user()

            // when
            val result = userService.updateCalorieAndRewardFood(userId, 250, today)

            // then
            assertEquals(0, result.rewardedFoodQuantity)
            verify(exactly = 0) { dailyInfoRepository.markPurposeAchieved(any()) }
        }

        @Test
        fun `목표를 처음 넘기면 달성 처리와 연속 달성을 함께 올려야 한다`() {
            // given
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 300) } returns 200
            every { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) } returns true
            every { userRepository.rewardDailyCalorie(userId, 1, true, any()) } returns user(purposeStrict = 1)

            // when
            val result = userService.updateCalorieAndRewardFood(userId, 300, today)

            // then
            assertEquals(1, result.rewardedFoodQuantity)
            assertEquals(0, result.rewardedToyQuantity)
            verify { userRepository.rewardDailyCalorie(userId, 1, true, any()) }
        }

        @Test
        fun `다른 요청이 이미 달성 처리했으면 연속 달성을 다시 올리지 않아야 한다`() {
            // given
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 300) } returns 200
            every { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) } returns false
            every { userRepository.rewardDailyCalorie(userId, 1, false, any()) } returns user()

            // when
            userService.updateCalorieAndRewardFood(userId, 300, today)

            // then
            verify { userRepository.rewardDailyCalorie(userId, 1, false, any()) }
        }

        @Test
        fun `연속 달성이 3의 배수가 되면 장난감을 지급하고 일일 정보에 기록해야 한다`() {
            // given
            val toyUser = user(toyQuantity = 1, purposeStrict = 3)
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 300) } returns 200
            every { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) } returns true
            every { userRepository.rewardDailyCalorie(userId, 1, true, any()) } returns user(purposeStrict = 3)
            every { userRepository.increaseItems(userId, toy = 1) } returns toyUser
            every { dailyInfoRepository.markToyGiven(dailyInfo.id) } returns Unit

            // when
            val result = userService.updateCalorieAndRewardFood(userId, 300, today)

            // then
            assertEquals(1, result.rewardedToyQuantity)
            assertEquals(toyUser, result.user)
            verify { dailyInfoRepository.markToyGiven(dailyInfo.id) }
        }

        @Test
        fun `장난감 지급이 실패해도 칼로리 갱신은 성공으로 응답해야 한다`() {
            // given
            val rewardedUser = user(purposeStrict = 3)
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 300) } returns 200
            every { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) } returns true
            every { userRepository.rewardDailyCalorie(userId, 1, true, any()) } returns rewardedUser
            every { userRepository.increaseItems(userId, toy = 1) } throws DataAccessResourceFailureException("db down")

            // when
            val result = userService.updateCalorieAndRewardFood(userId, 300, today)

            // then
            assertEquals(0, result.rewardedToyQuantity)
            assertEquals(rewardedUser, result.user)
        }

        @Test
        fun `유저 보상 반영이 실패하면 칼로리와 달성 처리를 되돌려 다음 요청에서 다시 받을 수 있어야 한다`() {
            // given
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 300) } returns 200
            every { dailyInfoRepository.markPurposeAchieved(dailyInfo.id) } returns true
            every { userRepository.rewardDailyCalorie(userId, 1, true, any()) } throws DataAccessResourceFailureException("db down")
            every { dailyInfoRepository.revertCalorie(dailyInfo.id, 300, 200) } returns Unit
            every { dailyInfoRepository.unmarkPurposeAchieved(dailyInfo.id) } returns Unit

            // when & then
            assertThrows<DataAccessResourceFailureException> {
                userService.updateCalorieAndRewardFood(userId, 300, today)
            }
            verify { dailyInfoRepository.revertCalorie(dailyInfo.id, 300, 200) }
            verify { dailyInfoRepository.unmarkPurposeAchieved(dailyInfo.id) }
        }

        @Test
        fun `칼로리가 오르지 않은 요청이 실패하면 칼로리를 되돌리지 않아야 한다`() {
            // given
            every { dailyInfoRepository.raiseCalorie(dailyInfo.id, 100) } returns 200
            every { userRepository.rewardDailyCalorie(userId, 0, false, any()) } throws DataAccessResourceFailureException("db down")

            // when & then
            assertThrows<DataAccessResourceFailureException> {
                userService.updateCalorieAndRewardFood(userId, 100, today)
            }
            verify(exactly = 0) { dailyInfoRepository.revertCalorie(any(), any(), any()) }
            verify(exactly = 0) { dailyInfoRepository.unmarkPurposeAchieved(any()) }
        }

        @Test
        fun `오늘 첫 갱신이고 어제 목표를 못 채웠으면 연속 달성을 DB에서 초기화해야 한다`() {
            // given
            val yesterday = DailyInfo.create(userId, "유저", "DOG", today.minusDays(1))
            val created = DailyInfo.create(userId, "유저", "DOG", today)
            every { dailyInfoRepository.findByUserIdAndDate(userId, today) } returns null
            every { dailyInfoRepository.findByUserIdAndDate(userId, today.minusDays(1)) } returns yesterday
            every { userRepository.resetPurposeStrict(userId) } returns Unit
            every { dailyInfoRepository.insert(any<DailyInfo>()) } returns created
            every { dailyInfoRepository.raiseCalorie(created.id, 50) } returns 0
            every { dailyInfoRepository.findById(created.id) } returns Optional.of(created)
            every { userRepository.rewardDailyCalorie(userId, 0, false, any()) } returns user()

            // when
            userService.updateCalorieAndRewardFood(userId, 50, today)

            // then
            verify { userRepository.resetPurposeStrict(userId) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        private fun user(
            foodQuantity: Int = 0,
            toyQuantity: Int = 0,
            purposeStrict: Int = 0,
            purposeCalorie: Int = 300,
        ) = User(
            id = userId,
            deviceToken = null,
            name = "유저",
            mainPetId = null,
            purposeCalorie = purposeCalorie,
            foodQuantity = foodQuantity,
            toyQuantity = toyQuantity,
            purposeStrict = purposeStrict,
        )
    }
}
