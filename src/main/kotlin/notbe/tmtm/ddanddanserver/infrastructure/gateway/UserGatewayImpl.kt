package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.respository.UserRepository
import org.springframework.stereotype.Component

@Component
class UserGatewayImpl(
    private val userRepository: UserRepository,
) : UserGateway {
    override fun save(user: User) = userRepository.save(UserEntity.fromDomain(user)).toDomain()

    override fun getById(userId: String) = userRepository.getReferenceById(userId).toDomain()
}
