package notbe.tmtm.ddanddanserver.domain.model.friend

import notbe.tmtm.ddanddanserver.domain.exception.InviteCodeExpiredException
import notbe.tmtm.ddanddanserver.domain.exception.InviteCodeSelfUseException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document("invite_codes")
@CompoundIndex(name = "code_unique", def = "{'code': 1}", unique = true)
@CompoundIndex(name = "inviter_active", def = "{'inviterId': 1, 'isActive': 1}")
class InviteCode private constructor(
    val id: ObjectId,
    private val code: String,
    private val inviterId: ObjectId,
    @Indexed(expireAfterSeconds = 0)
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(
            inviterId: ObjectId,
            validityHours: Long = 24,
        ): InviteCode =
            InviteCode(
                id = ObjectId(),
                code = generateUniqueCode(),
                inviterId = inviterId,
                expiresAt = LocalDateTime.now().plusHours(validityHours),
            )

        private fun generateUniqueCode(): String =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .uppercase()
    }

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun getCode(): String = code

    fun getInviterId(): ObjectId = inviterId

    fun validateUse(inviteeId: ObjectId) {
        if (isExpired()) {
            throw InviteCodeExpiredException()
        }
        if (inviterId == inviteeId) {
            throw InviteCodeSelfUseException()
        }
    }
}
