package notbe.tmtm.ddanddanserver.domain.usecase.notification

import notbe.tmtm.ddanddanserver.domain.gateway.PushGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotifyCheckCalorie(
    private val userGateway: UserGateway,
    private val pushGateway: PushGateway,
) : UseCase<Unit, Unit> {
    @Transactional(readOnly = true)
    override fun execute(input: Unit) {
        val allUsers =
            userGateway
                .findAll()
                .filter { it.setting.isAppPushOn }
                .filter { it.deviceToken.isNullOrEmpty().not() }
        pushGateway.sendAll(allUsers.map { it.deviceToken!! }, PushMessage.CHECK_CALORIE.content)
    }
}
