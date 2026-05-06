package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserAdminService(
    private val userRepository: UserRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val petRepository: PetRepository,
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

    fun getDailyCalories(
        userId: ObjectId,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyInfo> {
        // 유저 존재 확인 — 없으면 UserNotFoundException
        userRepository.findByIdOrThrow(userId)
        return dailyInfoRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, from, to)
    }

    /**
     * 여러 유저의 mainPet `PetType`을 한 번의 쿼리로 조회한다.
     * 목록 응답에서 N+1을 피하기 위해 사용. mainPetId가 없거나 펫이 삭제된 유저는 결과 맵에 포함되지 않는다.
     */
    fun getMainPetTypes(users: List<User>): Map<ObjectId, PetType> {
        val mainPetIds = users.mapNotNull { it.mainPetId }
        if (mainPetIds.isEmpty()) return emptyMap()
        return petRepository.findAllById(mainPetIds).associate { it.id to it.type }
    }

    /** 단일 유저의 mainPet `PetType`. mainPetId가 없거나 펫이 삭제된 경우 null. */
    fun getMainPetType(user: User): PetType? {
        val petId = user.mainPetId ?: return null
        return petRepository.findById(petId).orElse(null)?.type
    }
}
