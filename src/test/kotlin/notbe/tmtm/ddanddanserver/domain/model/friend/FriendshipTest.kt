package notbe.tmtm.ddanddanserver.domain.model.friend

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FriendshipTest {
    @Test
    fun `초대코드로 친구 관계를 생성할 수 있다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val inviteCode = "ABC12345"

        // when
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, inviteCode)

        // then
        assertEquals(inviterId, friendship.getInviterId())
        assertEquals(inviteeId, friendship.getInviteeId())
        assertEquals(inviteCode, friendship.getInviteCode())
        assertEquals(FriendStatus.ACCEPTED, friendship.getStatus())
        assertTrue(friendship.isAccepted())
        assertNotNull(friendship.getAcceptedAt())
    }

    @Test
    fun `자기 자신과는 친구가 될 수 없다`() {
        // given
        val userId = ObjectId()
        val inviteCode = "ABC12345"

        // when & then
        assertThrows<IllegalArgumentException> {
            Friendship.createFromInvite(userId, userId, inviteCode)
        }
    }

    @Test
    fun `친구 관계 확인이 정상 작동한다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertTrue(friendship.isFriendWith(inviterId))
        assertTrue(friendship.isFriendWith(inviteeId))
        assertFalse(friendship.isFriendWith(ObjectId()))
    }

    @Test
    fun `상대방 사용자 ID를 얻을 수 있다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertEquals(inviteeId, friendship.getOtherUserId(inviterId))
        assertEquals(inviterId, friendship.getOtherUserId(inviteeId))
    }

    @Test
    fun `관련되지 않은 사용자의 상대방 ID를 요청하면 예외가 발생한다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val otherUserId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertThrows<IllegalArgumentException> {
            friendship.getOtherUserId(otherUserId)
        }
    }

    @Test
    fun `초대자 확인이 정상 작동한다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertTrue(friendship.isInvitedBy(inviterId))
        assertFalse(friendship.isInvitedBy(inviteeId))
    }

    @Test
    fun `피초대자 확인이 정상 작동한다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertTrue(friendship.isInviteeOf(inviteeId))
        assertFalse(friendship.isInviteeOf(inviterId))
    }

    @Test
    fun `사용자 관련성 확인이 정상 작동한다`() {
        // given
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val otherUserId = ObjectId()
        val friendship = Friendship.createFromInvite(inviterId, inviteeId, "ABC12345")

        // when & then
        assertTrue(friendship.involvesUser(inviterId))
        assertTrue(friendship.involvesUser(inviteeId))
        assertFalse(friendship.involvesUser(otherUserId))
    }
}
