package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class WithdrawUser(
    private val userGateway: UserGateway,
    private val authGateway: AuthGateway,
    private val petGateway: PetGateway,
    private val dailyInfoGateway: DailyInfoGateway,
) : UseCase<WithdrawUser.Input, Unit> {
    data class Input(
        val userId: String,
    )

    override fun execute(input: Input) {
        userGateway.delete(input.userId)
        authGateway.deleteByUserId(input.userId)
        petGateway.deleteByUserId(input.userId)
        dailyInfoGateway.deleteByUserId(input.userId)
    }
}
