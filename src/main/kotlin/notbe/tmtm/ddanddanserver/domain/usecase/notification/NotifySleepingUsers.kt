package notbe.tmtm.ddanddanserver.domain.usecase.notification

import notbe.tmtm.ddanddanserver.domain.gateway.PushGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class NotifySleepingUsers(
    private val userGateway: UserGateway,
    private val pushGateway: PushGateway,
) : UseCase<Unit, Unit> {
    @Transactional(readOnly = true)
    override fun execute(input: Unit) {
        val now = LocalDate.now()
        val allUsers = userGateway.findAll().filter { it.setting.isAppPushOn }
        notifySleepingUser(allUsers.filter { it.lastLoginAt.isEqual(now.minusDays(7)) }.map { it.deviceToken })
        notifySleepingUser(allUsers.filter { it.lastLoginAt.isEqual(now.minusDays(14)) }.map { it.deviceToken })
        notifySleepingUser(allUsers.filter { it.lastLoginAt.isEqual(now.minusDays(30)) }.map { it.deviceToken })
    }

    private fun notifySleepingUser(deviceTokens: List<String>) {
        pushGateway.sendAll(deviceTokens, PushMessage.LONG_SLEEPING_USER.content)
    }
}
