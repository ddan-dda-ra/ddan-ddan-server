package notbe.tmtm.ddanddanserver.domain.usecase.user

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UpdateCalorieAndRewardFoodTest {
    private val userGateway: UserGateway = mockk<UserGateway>()
    private val dailyInfoGateway: DailyInfoGateway = mockk<DailyInfoGateway>()

    private val updateCalorieAndRewardFood = UpdateCalorieAndRewardFood(userGateway, dailyInfoGateway)

    @ParameterizedTest
    @CsvSource(
        value = [
            "5, 267, 267, 2",
            "101, 199, 199, 0",
            "100, 150, 150, 0",
            "500, 100, 500, 0",
            "99, 100, 100, 1",
        ],
    )
    fun `일일 칼로리 갱신에 성공하고, 먹이를 지급받는다`(
        previousCalorie: Int,
        requestCalorie: Int,
        expectCalorie: Int,
        expectFoodQuantity: Int,
    ) {
        // given
        val user = User.register(name = "userName")
        val dailyInfo = DailyInfo.register(userId = user.id, calorie = previousCalorie)
        every { userGateway.getById(user.id) } returns user
        every { userGateway.save(user) } returns user
        every { dailyInfoGateway.getOrCreate(user.id, dailyInfo.date) } returns dailyInfo
        every { dailyInfoGateway.save(dailyInfo) } returns dailyInfo

        // when
        val result =
            updateCalorieAndRewardFood.execute(
                UpdateCalorieAndRewardFood.Input(
                    userId = user.id,
                    calorie = requestCalorie,
                    today = dailyInfo.date,
                ),
            )

        // then
        Assertions.assertEquals(expectFoodQuantity, result.user.foodQuantity)
        Assertions.assertEquals(expectCalorie, result.dailyInfo.calorie)
    }
}
