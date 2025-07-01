package notbe.tmtm.ddanddanserver.domain.model.friend

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InviteCodeTest {
    @Test
    fun `초대코드를 생성할 수 있다`() {
        // given
        val inviterId = ObjectId()

        // when
        val inviteCode = InviteCode.create(inviterId)

        // then
        assertEquals(inviterId, inviteCode.getInviterId())
        assertEquals(8, inviteCode.getCode().length)
        assertTrue(inviteCode.getCode().matches(Regex("^[A-Z0-9]{8}$")))
        assertTrue(inviteCode.expiresAt.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `유효기간을 설정할 수 있다`() {
        // given
        val inviterId = ObjectId()
        val validityHours = 48L

        // when
        val inviteCode = InviteCode.create(inviterId, validityHours)

        // then
        val expectedExpiry = LocalDateTime.now().plusHours(validityHours)
        assertTrue(inviteCode.expiresAt.isAfter(expectedExpiry.minusMinutes(1)))
        assertTrue(inviteCode.expiresAt.isBefore(expectedExpiry.plusMinutes(1)))
    }
}
