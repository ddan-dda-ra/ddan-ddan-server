package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserMainPet
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserPetService(
    private val userRepository: UserRepository,
    private val petRepository: PetRepository,
    private val dailyInfoRepository: DailyInfoRepository,
) {
    @Transactional(readOnly = true)
    fun getMainPet(userId: ObjectId): Pet? {
        val mainPetId = userRepository.findByIdOrThrow(userId).mainPetId ?: return null

        return petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId)
    }

    @Transactional(readOnly = true)
    fun getUserMainPets(userIds: List<ObjectId>): List<UserMainPet> {
        val users = userRepository.findAllById(userIds)
        val pets = petRepository.findAllById(users.mapNotNull { it.mainPetId })

        return users.map { user ->
            UserMainPet(
                user = user,
                mainPet = pets.find { it.id == user.mainPetId }
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserMainPet(userId: ObjectId): UserMainPet {
        val user = userRepository.findByIdOrThrow(userId)
        val petId = user.mainPetId ?: return UserMainPet(user, null)
        return UserMainPet(
            user = user,
            mainPet = petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId)
        )
    }

    @Transactional
    fun setMainPet(ownerUserId: ObjectId, petId: ObjectId): Pair<User, Pet> {
        val user = userRepository.findByIdOrThrow(ownerUserId)
        val pet = petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId)

        user.setMainPet(pet.id)
        val savedUser = userRepository.save(user)

        dailyInfoRepository.findByUserIdAndDate(user.id, LocalDate.now())?.let {
            it.petType = pet.type
            dailyInfoRepository.save(it)
        }

        return Pair(savedUser, pet)
    }
}
