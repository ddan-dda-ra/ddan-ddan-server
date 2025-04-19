package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.max

private const val CALORIE_REWARD_UNIT = 100

@Component
class UpdateCalorieAndRewardFood(
    private val userGateway: UserGateway,
    private val petGateway: PetGateway,
    private val dailyInfoGateway: DailyInfoGateway,
    private val dailyInfoRepository: DailyInfoRepository,
) : UseCase<UpdateCalorieAndRewardFood.Input, UpdateCalorieAndRewardFood.Output> {
    data class Input(
        val userId: ObjectId,
        val calorie: Int,
        val today: LocalDate,
    )

    data class Output(
        val user: User,
        val dailyInfo: DailyInfo,
        val rewardedFoodQuantity: Int,
        val rewardedToyQuantity: Int,
    )

    @Transactional
    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.userId)
        val calorieDailyInfo = getOrCreateDailyInfo(user, input.today)
        val rewardFood = getRewardFood(calorieDailyInfo.calorie, input.calorie)
        var rewardedToyQuantity = 0
        user.foodQuantity += rewardFood

        if (isDailyPurposeAchieve(calorieDailyInfo, user, input.calorie)) {
            calorieDailyInfo.purposeAchieved = true
            user.toyQuantity++
            if (validateToyGiven(user.purposeStrict)) {
                rewardedToyQuantity = 1
                user.addPurposeStrict()
                calorieDailyInfo.toyGiven = true
            }
        }

        calorieDailyInfo.update(input.calorie)
        user.lastLoginAt = LocalDate.now()

        return Output(userGateway.save(user), dailyInfoGateway.save(calorieDailyInfo), rewardFood, rewardedToyQuantity)
    }

    private fun getOrCreateDailyInfo(
        user: User,
        today: LocalDate,
    ): DailyInfo {
        dailyInfoGateway.findBy(user.id, today)?.let { return it }

        val mainPetType = user.mainPetId?.let { petGateway.getById(it).type }
        return try {
            dailyInfoRepository.insert(DailyInfo.create(user.id, user.name, mainPetType, today))
        } catch (e: DuplicateKeyException) {
            dailyInfoGateway.findBy(user.id, today)!!
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
}
