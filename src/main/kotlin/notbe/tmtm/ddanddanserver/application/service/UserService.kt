package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val petRepository: PetRepository,
) {
    fun getAll(): List<User> = userRepository.findAll()

    fun getById(id: ObjectId): User? = userRepository.findByIdOrNull(id)

    fun getByIdOrThrow(id: ObjectId): User = getById(id) ?: throw UserNotFoundException("User not found with id: $id")

    fun update(user: User): User = userRepository.save(user)

    fun updateInfo(
        userId: ObjectId,
        name: String,
        purposeCalorie: Int,
    ): User {
        val user = getByIdOrThrow(userId)
        user.updateInfo(name, purposeCalorie)
        return update(user)
    }

    fun withdraw(userId: ObjectId) {
        authRepository.deleteAllByUserId(userId)
        petRepository.deleteAllByOwnerUserId(userId)
        dailyInfoRepository.deleteAllByUserId(userId)
        userRepository.deleteById(userId)
    }
}
