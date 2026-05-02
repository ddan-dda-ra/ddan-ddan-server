package notbe.tmtm.ddanddanserver.application.event

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class UserRegisteredEventListener(
    private val userRepository: UserRepository,
    private val discordHookApi: DiscordHookApi,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: UserRegisteredEvent) {
        try {
            val count = userRepository.count()
            val phase = activeProfile.uppercase()
            val registeredAtKst = "${KST_FORMATTER.format(event.registeredAt)} KST"
            val body =
                "[$phase] 신규 가입 | 닉네임=${event.nickName} | provider=${event.oAuthType} | " +
                    "userId=${event.userId} | 가입시각=$registeredAtKst | " +
                    "누적 가입자=${String.format("%,d", count)}명"

            discordHookApi.sendMessage(DiscordHookApi.Request(content = body))
        } catch (e: Exception) {
            logger().error(
                "Discord 신규 가입 알림 발송 실패: userId={}, provider={}",
                event.userId,
                event.oAuthType,
                e,
            )
        }
    }

    companion object {
        private val KST_FORMATTER: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Seoul"))
    }
}
