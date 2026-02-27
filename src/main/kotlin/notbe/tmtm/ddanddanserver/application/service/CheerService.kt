package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.CheerNotFriendsException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CheerService(
    private val cheerRepository: CheerRepository,
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository,
    private val pushClient: PushClient,
) {
    fun createCheer(
        cheererId: ObjectId,
        cheereeId: ObjectId,
    ): Cheer {
        val cheeree = userRepository.findByIdOrThrow(cheereeId)
        val cheerer = userRepository.findByIdOrThrow(cheererId)
        validateFriendship(cheererId, cheereeId)

        val cheer = Cheer.create(
            cheererId = cheererId,
            cheereeId = cheereeId,
            date = LocalDate.now(),
        )

        val result = cheerRepository.saveWithDuplicateCheck(cheer)
        sendCheerMessage(cheeree.deviceToken, cheerer.name)
        return result
    }

    private fun validateFriendship(
        userId1: ObjectId,
        userId2: ObjectId,
    ) {
        if (friendshipRepository.areFriends(userId1, userId2).not()) {
            throw CheerNotFriendsException()
        }
    }

    private fun sendCheerMessage(cheereeDeviceToken: DeviceToken?, cheererName: String?) {
        if (cheererName == null) return
        val message = createRandomCheerMessage(cheererName)
        pushClient.sendToUser(cheereeDeviceToken, message, RoutingView.MAIN)
    }

    private fun createRandomCheerMessage(cheererName: String): String {
        val messages = listOf(
            "${cheererName}님의 응원 버프가 도착했어요!",
            "지금도 멋지게 해내고 있어요! ${cheererName}님의 파워 응원 도착!",
            "${cheererName}님이 나를 응원했어요! 오늘의 목표 칼로리를 달성하러 가볼까요?",
        )
        return messages.random()
    }
}
