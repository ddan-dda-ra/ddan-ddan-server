package notbe.tmtm.ddanddanserver.application.service

import io.mockk.*
import notbe.tmtm.ddanddanserver.domain.exception.*
import notbe.tmtm.ddanddanserver.domain.model.friend.Friend
import notbe.tmtm.ddanddanserver.domain.model.friend.FriendStatus
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendServiceTest {
    private lateinit var friendRepository: FriendRepository
    private lateinit var userRepository: UserRepository
    private lateinit var friendService: FriendService

    private val userId1 = ObjectId()
    private val userId2 = ObjectId()

    @BeforeEach
    fun setUp() {
        friendRepository = mockk()
        userRepository = mockk()
        friendService = FriendService(friendRepository, userRepository)
        
        // Repository 확장 함수들을 mockk으로 처리
        mockkStatic("notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendRepositoryKt")
    }

    @Test
    fun `친구 신청 성공`() {
        // given
        val user1 = mockk<User>()
        val user2 = mockk<User>()
        every { userRepository.findByIdOrThrow(userId1) } returns user1
        every { userRepository.findByIdOrThrow(userId2) } returns user2
        every { friendRepository.findByRequesterIdAndReceiverId(userId1, userId2) } returns null
        every { friendRepository.findByRequesterIdAndReceiverId(userId2, userId1) } returns null
        every { friendRepository.save(any()) } returnsArgument 0

        // when
        val result = friendService.sendFriendRequest(userId1, userId2)

        // then
        assertEquals(userId1, result.getRequesterId())
        assertEquals(userId2, result.getReceiverId())
        assertEquals(FriendStatus.PENDING, result.getStatus())
        verify { friendRepository.save(any()) }
    }

    @Test
    fun `자기 자신에게 친구 신청 실패`() {
        // when & then
        assertThrows<FriendSelfAddException> {
            friendService.sendFriendRequest(userId1, userId1)
        }
    }

    @Test
    fun `이미 친구 관계인 경우 신청 실패`() {
        // given
        val user1 = mockk<User>()
        val user2 = mockk<User>()
        val existingFriend = mockk<Friend>()
        every { userRepository.findByIdOrThrow(userId1) } returns user1
        every { userRepository.findByIdOrThrow(userId2) } returns user2
        every { friendRepository.findByRequesterIdAndReceiverId(userId1, userId2) } returns existingFriend
        every { existingFriend.isAccepted() } returns true

        // when & then
        assertThrows<FriendAlreadyExistsException> {
            friendService.sendFriendRequest(userId1, userId2)
        }
    }

    @Test
    fun `이미 친구 신청한 경우 실패`() {
        // given
        val user1 = mockk<User>()
        val user2 = mockk<User>()
        val pendingRequest = mockk<Friend>()
        every { userRepository.findByIdOrThrow(userId1) } returns user1
        every { userRepository.findByIdOrThrow(userId2) } returns user2
        every { friendRepository.findByRequesterIdAndReceiverId(userId1, userId2) } returns pendingRequest
        every { pendingRequest.isAccepted() } returns false
        every { pendingRequest.isPending() } returns true

        // when & then
        assertThrows<FriendRequestAlreadyProcessedException> {
            friendService.sendFriendRequest(userId1, userId2)
        }
    }

    @Test
    fun `교차 요청 시 자동 수락`() {
        // given - B가 이미 A에게 친구 신청을 보낸 상태
        val user1 = mockk<User>()
        val user2 = mockk<User>()
        val reverseRequest = mockk<Friend>(relaxed = true)
        every { userRepository.findByIdOrThrow(userId1) } returns user1
        every { userRepository.findByIdOrThrow(userId2) } returns user2
        every { friendRepository.findByRequesterIdAndReceiverId(userId1, userId2) } returns null
        every { friendRepository.findByRequesterIdAndReceiverId(userId2, userId1) } returns reverseRequest
        every { reverseRequest.isPending() } returns true
        every { reverseRequest.accept() } returns reverseRequest
        every { reverseRequest.isAccepted() } returns true
        every { friendRepository.save(any()) } returnsArgument 0

        // when - A가 B에게 친구 신청
        val result = friendService.sendFriendRequest(userId1, userId2)

        // then - 자동으로 수락됨
        assertTrue(result.isAccepted())
        verify { friendRepository.save(any()) }
    }

    @Test
    fun `중복 키 예외 발생 시 적절한 예외 처리`() {
        // given
        val user1 = mockk<User>()
        val user2 = mockk<User>()
        every { userRepository.findByIdOrThrow(userId1) } returns user1
        every { userRepository.findByIdOrThrow(userId2) } returns user2
        every { friendRepository.findByRequesterIdAndReceiverId(userId1, userId2) } returns null
        every { friendRepository.findByRequesterIdAndReceiverId(userId2, userId1) } returns null
        every { friendRepository.save(any()) } throws DuplicateKeyException("Duplicate key")

        // when & then
        assertThrows<FriendAlreadyExistsException> {
            friendService.sendFriendRequest(userId1, userId2)
        }
    }

    @Test
    fun `친구 요청 수락 성공`() {
        // given
        val friendRequestId = ObjectId()
        val pendingRequest = mockk<Friend>(relaxed = true)
        every { friendRepository.findByIdOrThrow(friendRequestId) } returns pendingRequest
        every { pendingRequest.isReceivedBy(userId2) } returns true
        every { pendingRequest.accept() } returns pendingRequest
        every { pendingRequest.isAccepted() } returns true
        every { friendRepository.save(any()) } returnsArgument 0

        // when
        val result = friendService.acceptFriendRequest(friendRequestId, userId2)

        // then
        assertTrue(result.isAccepted())
        verify { friendRepository.save(any()) }
    }

    @Test
    fun `다른 사람의 친구 요청 수락 실패`() {
        // given
        val friendRequestId = ObjectId()
        val userId3 = ObjectId()
        val pendingRequest = mockk<Friend>()
        every { friendRepository.findByIdOrThrow(friendRequestId) } returns pendingRequest
        every { pendingRequest.isReceivedBy(userId3) } returns false

        // when & then
        assertThrows<FriendRequestNotFoundException> {
            friendService.acceptFriendRequest(friendRequestId, userId3)
        }
    }

    @Test
    fun `친구 요청 거부 성공`() {
        // given
        val friendRequestId = ObjectId()
        val pendingRequest = mockk<Friend>()
        every { friendRepository.findByIdOrThrow(friendRequestId) } returns pendingRequest
        every { pendingRequest.isReceivedBy(userId2) } returns true
        every { friendRepository.delete(any()) } just Runs

        // when
        friendService.rejectFriendRequest(friendRequestId, userId2)

        // then
        verify { friendRepository.delete(pendingRequest) }
    }
}