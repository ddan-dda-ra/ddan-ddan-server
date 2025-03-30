package notbe.tmtm.ddanddanserver.domain.usecase.notification

import notbe.tmtm.ddanddanserver.domain.gateway.PushGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotifyWeeklyRanking(
    private val userGateway: UserGateway,
    private val pushGateway: PushGateway,
) : UseCase<Int, Unit> {
    @Transactional(readOnly = true)
    override fun execute(input: Int) {
        val allUsers =
            userGateway
                .findAll()
                .filter { it.setting.isAppPushOn }
                .filter { it.deviceToken.isNullOrEmpty().not() }
                .map { it.deviceToken!! }
        pushGateway.sendAll(allUsers, "${PushMessage.WEEKLY_RANKING.content} $input 칼로리를 소모했대요.", RoutingView.MAIN)
    }
}
