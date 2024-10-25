package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.math.max

private const val CALORIE_REWARD_UNIT = 100

@Component
class UpdateCalorieAndRewardFood(
    private val userGateway: UserGateway,
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

    override fun execute(input: Input): Output {
        val calorieDailyInfo = dailyInfoGateway.getOrCreate(input.userId, input.today)
        val user =
            userGateway.getById(input.userId).apply { foodQuantity += getRewardFood(calorieDailyInfo.calorie, input.calorie) }

        if (isDailyPurposeAchieve(calorieDailyInfo, user, input.calorie)) {
            calorieDailyInfo.purposeAchieved = true
            val dailyInfosBefore2Days = dailyInfoGateway.getByDateBeforeNDays(input.userId, input.today, 2)
            if (validateToyGiven(dailyInfosBefore2Days)) {
                user.toyQuantity++
                calorieDailyInfo.toyGiven = true
            }
        }

        calorieDailyInfo.update(input.calorie)

        return Output(userGateway.save(user), dailyInfoGateway.save(calorieDailyInfo))
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
