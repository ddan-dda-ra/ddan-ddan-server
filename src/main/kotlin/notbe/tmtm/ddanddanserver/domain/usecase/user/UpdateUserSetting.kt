package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUserSetting.Input
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUserSetting.Output
import org.springframework.stereotype.Component

@Component
class UpdateUserSetting(
    private val userGateway: UserGateway,
) : UseCase<Input, Output> {
    data class Input(
        val userId: String,
        val isAppPushOn: Boolean?,
    )

    data class Output(
        val userSetting: UserSetting,
    )

    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.userId)
        user.setting =
            user.setting.copy(
                isAppPushOn = input.isAppPushOn ?: user.setting.isAppPushOn,
            )

        return Output(userGateway.update(user).setting)
    }
}
