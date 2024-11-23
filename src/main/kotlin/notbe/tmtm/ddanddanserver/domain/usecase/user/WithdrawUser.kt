package notbe.tmtm.ddanddanserver.domain.usecase.user

import jakarta.transaction.Transactional
import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
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

    @Transactional
    override fun execute(input: Input) {
        authGateway.deleteByUserId(input.userId)
        petGateway.deleteByUserId(input.userId)
        dailyInfoGateway.deleteByUserId(input.userId)
        userGateway.delete(input.userId)
    }
}
