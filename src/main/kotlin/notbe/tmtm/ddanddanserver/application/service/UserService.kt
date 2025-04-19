package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getAll(): List<User> = userRepository.findAll()

    fun getById(id: ObjectId): User? = userRepository.findByIdOrNull(id)

    fun update(user: User): User = userRepository.save(user)
}
