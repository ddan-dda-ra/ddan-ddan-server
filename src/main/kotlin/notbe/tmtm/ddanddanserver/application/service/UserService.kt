package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
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

    fun updateSetting(
        userId: ObjectId,
        isAppPushOn: Boolean?,
    ): UserSetting {
        val user = getByIdOrThrow(userId)
        user.setting = user.setting.copy(
            isAppPushOn = isAppPushOn ?: user.setting.isAppPushOn,
        )
        return update(user).setting
    }

    fun withdraw(userId: ObjectId) {
        authRepository.deleteAllByUserId(userId)
        petRepository.deleteAllByOwnerUserId(userId)
        dailyInfoRepository.deleteAllByUserId(userId)
        userRepository.deleteById(userId)
    }

    fun getMainPet(userId: ObjectId): notbe.tmtm.ddanddanserver.domain.model.pet.Pet? {
        val user = getById(userId)
        return user?.mainPetId?.let { petRepository.findByIdOrNull(it) }
    }

    fun setMainPet(
        ownerUserId: ObjectId,
        petId: ObjectId,
    ): notbe.tmtm.ddanddanserver.domain.model.pet.Pet {
        val user = getByIdOrThrow(ownerUserId)
        val pet = petRepository.findByIdOrNull(petId) ?: throw IllegalArgumentException("Pet not found with id: $petId")
        if (!pet.isOwner(user.id)) {
            throw notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException()
        }

        user.setMainPet(pet.id)
        userRepository.save(user)

        // 데일리 데이터에 펫 타입 갱신
        dailyInfoRepository.findByUserIdAndDate(userId = user.id, date = java.time.LocalDate.now())?.let {
            it.petType = pet.type
            dailyInfoRepository.save(it)
        }
        return pet
    }

    fun updateCalorieAndRewardFood(
        userId: ObjectId,
        calorie: Int,
        today: java.time.LocalDate,
    ): UpdateCalorieAndRewardFoodResult {
        val user = getByIdOrThrow(userId)
        val calorieDailyInfo = getOrCreateDailyInfo(user, today)
        val rewardFood = getRewardFood(calorieDailyInfo.calorie, calorie)
        var rewardedToyQuantity = 0
        user.foodQuantity += rewardFood

        if (isDailyPurposeAchieve(calorieDailyInfo, user, calorie)) {
            calorieDailyInfo.purposeAchieved = true
            user.addPurposeStrict()
            if (validateToyGiven(user.purposeStrict)) {
                user.toyQuantity++
                rewardedToyQuantity = 1
                calorieDailyInfo.toyGiven = true
            }
        }

        calorieDailyInfo.update(calorie)
        user.lastLoginAt = java.time.LocalDate.now()

        return UpdateCalorieAndRewardFoodResult(
            userRepository.save(user),
            dailyInfoRepository.save(calorieDailyInfo),
            rewardFood,
            rewardedToyQuantity,
        )
    }

    private fun getOrCreateDailyInfo(
        user: User,
        today: java.time.LocalDate,
    ): notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo {
        dailyInfoRepository.findByUserIdAndDate(user.id, today)?.let { return it }

        val mainPetType = user.mainPetId?.let { petRepository.findByIdOrNull(it)?.type }
        return try {
            if (dailyInfoRepository.findByUserIdAndDate(user.id, today.minusDays(1))?.purposeAchieved == false) {
                user.purposeStrict = 0
            }
            dailyInfoRepository.insert(notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo.create(user.id, user.name, mainPetType, today))
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            dailyInfoRepository.findByUserIdAndDate(user.id, today)!!
        }
    }

    private fun validateToyGiven(purposeStrict: Int): Boolean = purposeStrict != 0 && purposeStrict % 3 == 0

    private fun isDailyPurposeAchieve(
        calorieDailyInfo: notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo,
        user: User,
        currentCalorie: Int,
    ) = calorieDailyInfo.calorie < user.purposeCalorie && currentCalorie >= user.purposeCalorie

    private fun getRewardFood(
        previousCalorie: Int,
        currentCalorie: Int,
    ): Int = kotlin.math.max(currentCalorie / CALORIE_REWARD_UNIT - previousCalorie / CALORIE_REWARD_UNIT, 0)

    companion object {
        private const val CALORIE_REWARD_UNIT = 100
    }

    data class UpdateCalorieAndRewardFoodResult(
        val user: User,
        val dailyInfo: notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo,
        val rewardedFoodQuantity: Int,
        val rewardedToyQuantity: Int,
    )
}
