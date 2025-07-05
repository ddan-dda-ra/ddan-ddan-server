package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.max

@Service
class UserService(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val petRepository: PetRepository,
) {
    fun getAll(): List<User> = userRepository.findAll()

    fun getAllByIds(ids: List<ObjectId>): List<User> = userRepository.findAllById(ids)

    fun getByIdOrThrow(id: ObjectId): User = userRepository.findByIdOrThrow(id)

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
        user.setting =
            user.setting.copy(
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

    data class CalorieUpdateResult(
        val user: User,
        val dailyInfo: DailyInfo,
        val rewardedFoodQuantity: Int,
        val rewardedToyQuantity: Int,
    )

    @Transactional
    fun updateCalorieAndRewardFood(
        userId: ObjectId,
        calorie: Int,
        today: LocalDate,
    ): CalorieUpdateResult {
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
        user.lastLoginAt = LocalDate.now()

        return CalorieUpdateResult(
            update(user),
            dailyInfoRepository.save(calorieDailyInfo),
            rewardFood,
            rewardedToyQuantity,
        )
    }

    private fun getOrCreateDailyInfo(
        user: User,
        today: LocalDate,
    ): DailyInfo {
        dailyInfoRepository.findByUserIdAndDate(user.id, today)?.let { return it }

        val mainPetType = user.mainPetId?.let { petRepository.findByIdOrNull(it)?.type }
        return try {
            if (dailyInfoRepository.findByUserIdAndDate(user.id, today.minusDays(1))?.purposeAchieved == false) {
                user.purposeStrict = 0
            }
            dailyInfoRepository.insert(DailyInfo.create(user.id, user.name, mainPetType, today))
        } catch (e: DuplicateKeyException) {
            dailyInfoRepository.findByUserIdAndDate(user.id, today)!!
        }
    }

    private fun validateToyGiven(purposeStrict: Int): Boolean = purposeStrict != 0 && purposeStrict % 3 == 0

    private fun isDailyPurposeAchieve(
        calorieDailyInfo: DailyInfo,
        user: User,
        currentCalorie: Int,
    ) = calorieDailyInfo.calorie < user.purposeCalorie && currentCalorie >= user.purposeCalorie

    private fun getRewardFood(
        previousCalorie: Int,
        currentCalorie: Int,
    ): Int = max(currentCalorie / CALORIE_REWARD_UNIT - previousCalorie / CALORIE_REWARD_UNIT, 0)

    companion object {
        private const val CALORIE_REWARD_UNIT = 100
    }
}
