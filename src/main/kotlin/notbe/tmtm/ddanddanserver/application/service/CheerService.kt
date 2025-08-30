package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.CheerNotFriendsException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CheerService(
    private val cheerRepository: CheerRepository,
    private val friendshipRepository: FriendshipRepository,
) {
    fun createCheer(
        cheererId: ObjectId,
        cheereeId: ObjectId,
    ): Cheer {
        validateFriendship(cheererId, cheereeId)

        val cheer = Cheer.create(
            cheererId = cheererId,
            cheereeId = cheereeId,
            date = LocalDate.now(),
        )

        return cheerRepository.saveWithDuplicateCheck(cheer)
    }

    fun getMonthlyReceivedCheerCount(userId: ObjectId): Long {
        return cheerRepository.countMonthlyReceivedCheers(userId)
    }

    private fun validateFriendship(
        userId1: ObjectId,
        userId2: ObjectId,
    ) {
        if (friendshipRepository.areFriends(userId1, userId2).not()) {
            throw CheerNotFriendsException()
        }
    }
}
