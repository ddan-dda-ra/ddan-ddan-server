package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

interface UserRepository : MongoRepository<User, ObjectId>, UserRepositoryCustom {
    fun findByNameContainingIgnoreCase(
        keyword: String,
        pageable: Pageable,
    ): Page<User>
}

fun UserRepository.findByIdOrThrow(userId: ObjectId): User =
    findByIdOrNull(userId) ?: throw UserNotFoundException("User not found with id: $userId")
