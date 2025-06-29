package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.FriendNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.friend.Friend
import notbe.tmtm.ddanddanserver.domain.model.friend.FriendStatus
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

interface FriendRepository : MongoRepository<Friend, ObjectId> {
    fun findByRequesterIdAndReceiverId(requesterId: ObjectId, receiverId: ObjectId): Friend?
    fun findByRequesterIdOrReceiverId(requesterId: ObjectId, receiverId: ObjectId): List<Friend>
    fun findByRequesterIdAndStatus(requesterId: ObjectId, status: FriendStatus): List<Friend>
    fun findByReceiverIdAndStatus(receiverId: ObjectId, status: FriendStatus): List<Friend>
}

fun FriendRepository.findByIdOrThrow(id: ObjectId): Friend {
    return findByIdOrNull(id) ?: throw FriendNotFoundException()
}

fun FriendRepository.findFriendshipBetween(userId1: ObjectId, userId2: ObjectId): Friend? {
    return findByRequesterIdAndReceiverId(userId1, userId2)
        ?: findByRequesterIdAndReceiverId(userId2, userId1)
}

fun FriendRepository.findAcceptedFriends(userId: ObjectId): List<Friend> {
    return findByRequesterIdOrReceiverId(userId, userId)
        .filter { it.isAccepted() }
}

fun FriendRepository.findPendingRequestsReceived(userId: ObjectId): List<Friend> {
    return findByReceiverIdAndStatus(userId, FriendStatus.PENDING)
}

fun FriendRepository.findPendingRequestsSent(userId: ObjectId): List<Friend> {
    return findByRequesterIdAndStatus(userId, FriendStatus.PENDING)
}
