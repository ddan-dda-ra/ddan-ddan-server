package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
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
