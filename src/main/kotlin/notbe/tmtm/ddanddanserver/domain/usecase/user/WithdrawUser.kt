package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.infrastructure.api.SlackHookApi
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WithdrawUser(
    private val userGateway: UserGateway,
    private val authGateway: AuthGateway,
    private val petGateway: PetGateway,
    private val dailyInfoGateway: DailyInfoGateway,
    private val slackHookApi: SlackHookApi,
) : UseCase<WithdrawUser.Input, Unit> {
    val logger = logger()

    data class Input(
        val userId: ObjectId,
        val cause: String,
    )

    @Transactional
    override fun execute(input: Input) {
        authGateway.deleteByUserId(input.userId)
        petGateway.deleteByUserId(input.userId)
        dailyInfoGateway.deleteByUserId(input.userId)
        userGateway.delete(input.userId)

        runCatching {
            slackHookApi.sendMessage(
                SlackHookApi.Request(
                    channel = SlackHookApi.WITHDRAW_ALERT_CHANNEL,
                    text = "탈퇴 처리 완료: ${input.userId} \n 사유: ${input.cause}",
                ),
            )
        }.onFailure { e ->
            logger.error("slackHookClient.sendMessage error: ${e.message}")
        }
    }
}
