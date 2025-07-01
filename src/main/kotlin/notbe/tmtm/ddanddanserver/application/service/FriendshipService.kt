package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.FriendshipAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.exception.FriendshipNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.FriendshipSelfAddException
import notbe.tmtm.ddanddanserver.domain.exception.InviteCodeNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
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
) {
    @Transactional
    fun addFriendByInviteCode(
        code: String,
        inviteeId: ObjectId,
    ): Friendship {
        val inviteCode = inviteCodeRepository.findByCode(code) ?: throw InviteCodeNotFoundException()
        val inviterId = inviteCode.getInviterId()

        inviteCode.validateUse(inviteeId)
        validateFriendship(inviterId, inviteeId)

        val friendship =
            Friendship.createFromInvite(
                inviterId = inviterId,
                inviteeId = inviteeId,
                inviteCode = code,
            )

        return try {
            friendshipRepository.save(friendship)
        } catch (e: DuplicateKeyException) {
            throw FriendshipAlreadyExistsException()
        }
    }

    @Transactional(readOnly = true)
    fun getFriends(userId: ObjectId): List<Friendship> = friendshipRepository.findAcceptedFriends(userId)

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

        userRepository.findByIdOrThrow(inviterId)

        if (friendshipRepository.areFriends(inviterId, inviteeId)) {
            throw FriendshipAlreadyExistsException()
        }
    }
}
