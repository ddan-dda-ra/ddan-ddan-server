package notbe.tmtm.ddanddanserver.domain.model.friend

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FriendTest {
    private val userId1 = ObjectId()
    private val userId2 = ObjectId()
    private val userId3 = ObjectId()

    @Test
    fun `친구 요청 생성 성공`() {
        // when
        val friend = Friend.createRequest(userId1, userId2)

        // then
        assertEquals(userId1, friend.getRequesterId())
        assertEquals(userId2, friend.getReceiverId())
        assertEquals(FriendStatus.PENDING, friend.getStatus())
        assertTrue(friend.isPending())
        assertFalse(friend.isAccepted())
    }

    @Test
    fun `자기 자신에게 친구 요청 생성 실패`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Friend.createRequest(userId1, userId1)
        }
    }

    @Test
    fun `친구 요청 수락 성공`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when
        val acceptedFriend = friend.accept()

        // then
        assertTrue(acceptedFriend.isAccepted())
        assertFalse(acceptedFriend.isPending())
        assertEquals(FriendStatus.ACCEPTED, acceptedFriend.getStatus())
    }

    @Test
    fun `이미 수락된 요청 재수락 실패`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)
        friend.accept()

        // when & then
        assertThrows<IllegalArgumentException> {
            friend.accept()
        }
    }

    @Test
    fun `친구 관계 확인 - 수락된 경우`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)
        friend.accept()

        // when & then
        assertTrue(friend.isFriendWith(userId1))
        assertTrue(friend.isFriendWith(userId2))
        assertFalse(friend.isFriendWith(userId3))
    }

    @Test
    fun `친구 관계 확인 - 대기중인 경우`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertFalse(friend.isFriendWith(userId1))
        assertFalse(friend.isFriendWith(userId2))
    }

    @Test
    fun `상대방 사용자 ID 조회`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertEquals(userId2, friend.getOtherUserId(userId1))
        assertEquals(userId1, friend.getOtherUserId(userId2))
    }

    @Test
    fun `관련 없는 사용자 ID로 상대방 조회 실패`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertThrows<IllegalArgumentException> {
            friend.getOtherUserId(userId3)
        }
    }

    @Test
    fun `요청자 확인`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertTrue(friend.isRequestedBy(userId1))
        assertFalse(friend.isRequestedBy(userId2))
        assertFalse(friend.isRequestedBy(userId3))
    }

    @Test
    fun `수신자 확인`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertTrue(friend.isReceivedBy(userId2))
        assertFalse(friend.isReceivedBy(userId1))
        assertFalse(friend.isReceivedBy(userId3))
    }

    @Test
    fun `사용자 참여 확인`() {
        // given
        val friend = Friend.createRequest(userId1, userId2)

        // when & then
        assertTrue(friend.involvesUser(userId1))
        assertTrue(friend.involvesUser(userId2))
        assertFalse(friend.involvesUser(userId3))
    }
}
