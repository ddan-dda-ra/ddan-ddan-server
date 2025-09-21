package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserProfile
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val petRepository: PetRepository,
    private val cheerRepository: CheerRepository,
    private val friendshipRepository: FriendshipRepository,
) {
    fun getUserProfile(userId: ObjectId, myId: ObjectId): UserProfile {
        val user = userRepository.findByIdOrThrow(userId)
        val mainPet = petRepository.findByIdAndOwnerUserIdOrThrow(user.getMainPetIdOrThrow(), userId)
        val todayDailyInfo = getTodayDailyInfoOrDefault(userId, user, mainPet)
        val receivedCheers = cheerRepository.findAllByCheereeIdAndThisMonth(userId)
        val isFriend = friendshipRepository.areFriends(userId, myId)

        return UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = receivedCheers,
            isFriend = isFriend,
        )
    }

    private fun getTodayDailyInfoOrDefault(
        userId: ObjectId,
        user: User,
        mainPet: Pet
    ): DailyInfo = dailyInfoRepository.findByUserIdAndDate(userId, LocalDate.now()) ?: DailyInfo.create(
        userId,
        user.name,
        mainPet.type
    )
}
