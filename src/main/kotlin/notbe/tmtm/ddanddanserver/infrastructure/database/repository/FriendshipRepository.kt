package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.FriendshipNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.friend.FriendStatus
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.findByIdOrNull

interface FriendshipRepository : MongoRepository<Friendship, ObjectId> {
    fun findByInviterIdAndInviteeId(
        inviterId: ObjectId,
        inviteeId: ObjectId,
    ): Friendship?

    @Query("{ '\$or': [ { 'inviterId': ?0, 'status': ?1 }, { 'inviteeId': ?0, 'status': ?1 } ] }")
    fun findByUserIdAndStatus(
        userId: ObjectId,
        status: FriendStatus,
    ): List<Friendship>

    fun deleteAllByInviterId(inviterId: ObjectId)

    fun deleteAllByInviteeId(inviteeId: ObjectId)
}

fun FriendshipRepository.findByIdOrThrow(friendId: ObjectId): Friendship =
    findByIdOrNull(friendId)
        ?: throw FriendshipNotFoundException()

fun FriendshipRepository.findFriendshipBetween(
    userId1: ObjectId,
    userId2: ObjectId,
): Friendship? =
    findByInviterIdAndInviteeId(userId1, userId2)
        ?: findByInviterIdAndInviteeId(userId2, userId1)

fun FriendshipRepository.findAcceptedFriends(userId: ObjectId): List<Friendship> =
    findByUserIdAndStatus(userId, FriendStatus.ACCEPTED)

fun FriendshipRepository.areFriends(
    userId1: ObjectId,
    userId2: ObjectId,
): Boolean {
    val friendship = findFriendshipBetween(userId1, userId2)
    return friendship?.isAccepted() == true
}

fun FriendshipRepository.deleteAllBy(userId: ObjectId) {
    deleteAllByInviterId(userId)
    deleteAllByInviteeId(userId)
}
