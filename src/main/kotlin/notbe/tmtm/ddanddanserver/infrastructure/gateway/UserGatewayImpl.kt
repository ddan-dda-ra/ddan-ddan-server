package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class UserGatewayImpl(
    private val userRepository: UserRepository,
) : UserGateway {
    override fun save(user: User) = userRepository.save(user)

    override fun findAll(): List<User> = userRepository.findAll()

    override fun getById(userId: ObjectId): User = userRepository.findById(userId).orElseThrow()

    override fun update(user: User) = userRepository.save(user)

    override fun delete(userId: ObjectId) = userRepository.deleteById(userId)
}
