package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.Test
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

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { userRepository.save(user) } returns updatedUser

        // when
        val result = userService.updateInfo(userId, newName, newPurposeCalorie)

        // then
        verify { user.updateInfo(newName, newPurposeCalorie) }
        verify { userRepository.save(user) }
        assertEquals(updatedUser, result)
    }

    @Test
    fun `사용자 설정 업데이트가 성공해야 한다`() {
        // given
        val userId = ObjectId()
        val oldSetting = UserSetting(isAppPushOn = false)
        val newSetting = UserSetting(isAppPushOn = true)
        val user =
            mockk<User>(relaxed = true) {
                every { setting } returnsMany listOf(oldSetting, newSetting)
            }
        val updatedUser =
            mockk<User> {
                every { setting } returns newSetting
            }

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { userRepository.save(user) } returns updatedUser

        // when
        val result = userService.updateSetting(userId, true)

        // then
        verify { userRepository.save(user) }
        assertEquals(newSetting, result)
    }

    @Test
    fun `사용자 탈퇴가 성공해야 한다`() {
        // given
        val userId = ObjectId()

        every { authRepository.deleteAllByUserId(userId) } returns Unit
        every { petRepository.deleteAllByOwnerUserId(userId) } returns Unit
        every { dailyInfoRepository.deleteAllByUserId(userId) } returns Unit
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
    fun `사용자 업데이트가 성공해야 한다`() {
        // given
        val user = mockk<User>()
        val savedUser = mockk<User>()

        every { userRepository.save(user) } returns savedUser

        // when
        val result = userService.update(user)

        // then
        verify { userRepository.save(user) }
        assertEquals(savedUser, result)
    }
}
