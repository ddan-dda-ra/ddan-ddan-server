package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
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
) : UseCase<UpdateCalorieAndRewardFood.Input, UpdateCalorieAndRewardFood.Output> {
    data class Input(
        val userId: String,
        val calorie: Int,
        val today: LocalDate,
    )

    data class Output(
        val user: User,
        val dailyInfo: DailyInfo,
    )

    @Transactional
    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.userId)
        val calorieDailyInfo = getOrCreateDailyInfo(user, input.today)
        user.foodQuantity += getRewardFood(calorieDailyInfo.calorie, input.calorie)

        if (isDailyPurposeAchieve(calorieDailyInfo, user, input.calorie)) {
            calorieDailyInfo.purposeAchieved = true
            val dailyInfosBefore2Days = dailyInfoGateway.getByDateBeforeNDays(input.userId, input.today, 2)
            if (validateToyGiven(dailyInfosBefore2Days)) {
                user.toyQuantity++
                calorieDailyInfo.toyGiven = true
            }
        }

        calorieDailyInfo.update(input.calorie)
        user.lastLoginAt = LocalDate.now()

        return Output(userGateway.save(user), dailyInfoGateway.save(calorieDailyInfo))
    }

    private fun getOrCreateDailyInfo(
        user: User,
        today: LocalDate,
    ): DailyInfo {
        dailyInfoGateway.findBy(user.id, today)?.let { return it }

        val mainPetType = user.mainPetId?.let { petGateway.getById(it).type }
        return DailyInfo.create(user.id, user.name, mainPetType, today)
    }

    private fun validateToyGiven(dailyInfos: List<DailyInfo>): Boolean {
        if (dailyInfos.size != 2) return false
        dailyInfos.forEach {
            if (it.purposeAchieved.not()) return false
            if (it.toyGiven) return false
        }
        return true
    }

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
