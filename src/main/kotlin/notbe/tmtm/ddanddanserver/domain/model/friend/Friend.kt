package notbe.tmtm.ddanddanserver.domain.model.friend

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("friends")
@CompoundIndex(name = "unique_friend_request", def = "{'requesterId': 1, 'receiverId': 1}", unique = true)
class Friend private constructor(
    val id: ObjectId,
    private val requesterId: ObjectId,
    private val receiverId: ObjectId,
    private var status: FriendStatus,
    val createdAt: LocalDateTime,
    private var updatedAt: LocalDateTime,
) {
    
    // 정적 팩토리 메서드
    companion object {
        fun createRequest(requesterId: ObjectId, receiverId: ObjectId): Friend {
            require(requesterId != receiverId) { "Cannot send friend request to yourself" }
            
            return Friend(
                id = ObjectId(),
                requesterId = requesterId,
                receiverId = receiverId,
                status = FriendStatus.PENDING,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
    
    // 상태 조회 메서드들
    fun getStatus(): FriendStatus = status
    fun getRequesterId(): ObjectId = requesterId
    fun getReceiverId(): ObjectId = receiverId
    fun getUpdatedAt(): LocalDateTime = updatedAt
    
    fun isAccepted(): Boolean = status == FriendStatus.ACCEPTED
    fun isPending(): Boolean = status == FriendStatus.PENDING
    
    // 비즈니스 로직 메서드들
    fun accept(): Friend {
        require(isPending()) { "Can only accept pending friend requests" }
        
        return this.apply {
            status = FriendStatus.ACCEPTED
            updatedAt = LocalDateTime.now()
        }
    }


    fun isFriendWith(userId: ObjectId): Boolean {
        return isAccepted() && (requesterId == userId || receiverId == userId)
    }

    fun getOtherUserId(userId: ObjectId): ObjectId {
        require(requesterId == userId || receiverId == userId) { 
            "User $userId is not part of this friendship" 
        }
        return if (requesterId == userId) receiverId else requesterId
    }
    
    fun isRequestedBy(userId: ObjectId): Boolean = requesterId == userId
    fun isReceivedBy(userId: ObjectId): Boolean = receiverId == userId
    fun involvesUser(userId: ObjectId): Boolean = requesterId == userId || receiverId == userId
}
