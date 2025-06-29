package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.*
import notbe.tmtm.ddanddanserver.domain.model.friend.Friend
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun sendFriendRequest(requesterId: ObjectId, receiverId: ObjectId): Friend {
        validateFriendRequest(requesterId, receiverId)

        // 교차 요청 확인 및 자동 수락 처리
        val reverseRequest = friendRepository.findByRequesterIdAndReceiverId(receiverId, requesterId)
        if (reverseRequest != null && reverseRequest.isPending()) {
            val acceptedRequest = reverseRequest.accept()
            return friendRepository.save(acceptedRequest)
        }

        // 새로운 친구 요청 생성
        val friend = Friend.createRequest(requesterId, receiverId)
        return try {
            friendRepository.save(friend)
        } catch (e: DuplicateKeyException) {
            throw FriendAlreadyExistsException()
        }
    }

    @Transactional
    fun acceptFriendRequest(requestId: ObjectId, userId: ObjectId): Friend {
        val friendRequest = friendRepository.findByIdOrThrow(requestId)
        
        if (!friendRequest.isReceivedBy(userId)) {
            throw FriendRequestNotFoundException()
        }
        
        val acceptedRequest = friendRequest.accept()
        return friendRepository.save(acceptedRequest)
    }

    @Transactional
    fun rejectFriendRequest(requestId: ObjectId, userId: ObjectId) {
        val friendRequest = friendRepository.findByIdOrThrow(requestId)
        
        if (!friendRequest.isReceivedBy(userId)) {
            throw FriendRequestNotFoundException()
        }
        
        // 거부된 요청은 바로 삭제 (재신청 가능하도록)
        friendRepository.delete(friendRequest)
    }

    @Transactional
    fun removeFriend(userId: ObjectId, friendId: ObjectId) {
        val friendship = friendRepository.findFriendshipBetween(userId, friendId)
            ?: throw FriendNotFoundException()
        
        if (!friendship.isAccepted()) {
            throw FriendNotFoundException()
        }
        
        friendRepository.delete(friendship)
    }

    fun getFriends(userId: ObjectId): List<FriendWithUser> {
        val friendships = friendRepository.findAcceptedFriends(userId)
        
        return friendships.map { friendship ->
            val friendUserId = friendship.getOtherUserId(userId)
            val friendUser = userRepository.findByIdOrThrow(friendUserId)
            FriendWithUser(friendship, friendUser)
        }
    }

    fun getPendingRequestsReceived(userId: ObjectId): List<FriendRequestWithUser> {
        val requests = friendRepository.findPendingRequestsReceived(userId)
        
        return requests.map { request ->
            val requesterUser = userRepository.findByIdOrThrow(request.getRequesterId())
            FriendRequestWithUser(request, requesterUser)
        }
    }

    fun areFriends(userId1: ObjectId, userId2: ObjectId): Boolean {
        val friendship = friendRepository.findFriendshipBetween(userId1, userId2)
        return friendship?.isAccepted() == true
    }

    private fun validateFriendRequest(requesterId: ObjectId, receiverId: ObjectId) {
        if (requesterId == receiverId) {
            throw FriendSelfAddException()
        }
        
        userRepository.findByIdOrThrow(requesterId)
        userRepository.findByIdOrThrow(receiverId)
        
        // 직접적인 친구 관계 확인 (A → B)
        val directRequest = friendRepository.findByRequesterIdAndReceiverId(requesterId, receiverId)
        if (directRequest != null) {
            when {
                directRequest.isAccepted() -> throw FriendAlreadyExistsException()
                directRequest.isPending() -> throw FriendRequestAlreadyProcessedException()
                // REJECTED 상태는 거부 시 즉시 삭제되므로 여기서 처리할 필요 없음
            }
        }
    }

    data class FriendWithUser(
        val friendship: Friend,
        val user: User,
    )

    data class FriendRequestWithUser(
        val request: Friend,
        val requester: User,
    )
}
