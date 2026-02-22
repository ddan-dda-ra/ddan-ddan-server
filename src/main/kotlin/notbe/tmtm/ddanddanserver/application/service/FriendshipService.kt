package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.FriendshipAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.exception.FriendshipNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.FriendshipSelfAddException
import notbe.tmtm.ddanddanserver.domain.exception.InviteCodeNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findAcceptedFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findFriendshipBetween
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FriendshipService(
    private val friendshipRepository: FriendshipRepository,
    private val inviteCodeRepository: InviteCodeRepository,
    private val userRepository: UserRepository,
    private val pushClient: PushClient
) {
    @Transactional
    fun addFriendByInviteCode(
        code: String,
        inviteeId: ObjectId,
    ): Friendship {
        val inviteCode = inviteCodeRepository.findByCode(code) ?: throw InviteCodeNotFoundException()
        val inviterId = inviteCode.getInviterId()
        val inviter = userRepository.findByIdOrThrow(inviterId)
        val invitee = userRepository.findByIdOrThrow(inviteeId)

        inviteCode.validateUse(inviteeId)
        validateFriendship(inviterId, inviteeId)

        val friendship =
            Friendship.createFromInvite(
                inviterId = inviterId,
                inviteeId = inviteeId,
                inviteCode = code,
            )

        return try {
            friendshipRepository.save(friendship).also {
                val message = "${invitee.name}님과 친구가 되었습니다!"
                pushClient.sendToUser(inviter.deviceToken, message, RoutingView.MAIN)
            }
        } catch (e: DuplicateKeyException) {
            throw FriendshipAlreadyExistsException()
        }
    }

    @Transactional(readOnly = true)
    fun getFriendIds(userId: ObjectId): List<ObjectId> =
        friendshipRepository.findAcceptedFriends(userId).map { it.getOtherUserId(userId) }

    @Transactional
    fun removeFriend(
        friendId: ObjectId,
        userId: ObjectId,
    ) {
        userRepository.findByIdOrThrow(friendId)
        val friendShip =
            friendshipRepository.findFriendshipBetween(userId, friendId)
                ?: throw FriendshipNotFoundException()

        friendshipRepository.delete(friendShip)
    }

    private fun validateFriendship(
        inviterId: ObjectId,
        inviteeId: ObjectId,
    ) {
        if (inviterId == inviteeId) {
            throw FriendshipSelfAddException()
        }
        if (friendshipRepository.areFriends(inviterId, inviteeId)) {
            throw FriendshipAlreadyExistsException()
        }
    }
}
