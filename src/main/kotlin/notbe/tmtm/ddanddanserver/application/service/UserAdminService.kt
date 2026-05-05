package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class UserAdminService(
    private val userRepository: UserRepository,
) {
    fun searchUsers(
        keyword: String?,
        sortType: UserAdminSortType,
        page: Int,
        size: Int,
    ): Page<User> {
        val pageable = PageRequest.of(page, size, sortType.sort)
        val trimmed = keyword?.trim()
        return if (trimmed.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            userRepository.findByNameContainingIgnoreCase(trimmed, pageable)
        }
    }

    fun getUser(userId: ObjectId): User = userRepository.findByIdOrThrow(userId)
}
