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

        calorieDailyInfo.update(input.calorie)

        return Output(userGateway.save(user), dailyInfoGateway.save(calorieDailyInfo))
    }

    private fun getRewardFood(
        previousCalorie: Int,
        currentCalorie: Int,
    ): Int = max(currentCalorie / CALORIE_REWARD_UNIT - previousCalorie / CALORIE_REWARD_UNIT, 0)
}
