package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserGatewayImpl(
    private val userRepository: UserRepository,
) : UserGateway {
    override fun getById(id: ObjectId): User =
        userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User not found with id: $id")

    override fun save(user: User): User = userRepository.save(user)

    override fun update(user: User): User = userRepository.save(user)

    override fun findAll(): List<User> = userRepository.findAll()
}
