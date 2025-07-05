package notbe.tmtm.ddanddanserver.domain.model.friend

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("friendships")
@CompoundIndex(name = "inviter_invitee_unique", def = "{'inviterId': 1, 'inviteeId': 1}", unique = true)
@CompoundIndex(name = "user_friends_inviter", def = "{'inviterId': 1, 'status': 1}")
@CompoundIndex(name = "user_friends_invitee", def = "{'inviteeId': 1, 'status': 1}")
class Friendship private constructor(
    val id: ObjectId,
    private val inviterId: ObjectId,
    private val inviteeId: ObjectId,
    private var status: FriendStatus,
    private val inviteCode: String?,
    val createdAt: LocalDateTime,
    private var acceptedAt: LocalDateTime?,
) {
    companion object {
        fun createFromInvite(
            inviterId: ObjectId,
            inviteeId: ObjectId,
            inviteCode: String,
        ): Friendship {
            require(inviterId != inviteeId) { "Cannot befriend yourself" }

            return Friendship(
                id = ObjectId(),
                inviterId = inviterId,
                inviteeId = inviteeId,
                status = FriendStatus.ACCEPTED,
                inviteCode = inviteCode,
                createdAt = LocalDateTime.now(),
                acceptedAt = LocalDateTime.now(),
            )
        }
    }

    fun getStatus(): FriendStatus = status

    fun getInviterId(): ObjectId = inviterId

    fun getInviteeId(): ObjectId = inviteeId

    fun getInviteCode(): String? = inviteCode

    fun getAcceptedAt(): LocalDateTime? = acceptedAt

    fun isAccepted(): Boolean = status == FriendStatus.ACCEPTED

    fun isFriendWith(userId: ObjectId): Boolean = isAccepted() && (inviterId == userId || inviteeId == userId)

    fun getOtherUserId(userId: ObjectId): ObjectId {
        require(inviterId == userId || inviteeId == userId) {
            "User $userId is not part of this friendship"
        }
        return if (inviterId == userId) inviteeId else inviterId
    }

    fun isInvitedBy(userId: ObjectId): Boolean = inviterId == userId

    fun isInviteeOf(userId: ObjectId): Boolean = inviteeId == userId

    fun involvesUser(userId: ObjectId): Boolean = inviterId == userId || inviteeId == userId
}
