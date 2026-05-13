package notbe.tmtm.ddanddanserver.application.event

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handle(event: UserRegisteredEvent) {
        // dev 프로파일은 실제 가입자 트래킹 대상이 아니라 테스트성 가입이 많아
        // Discord 채널 노이즈가 크다. 운영 가시성은 prod 에만 필요하므로 dev 에서는 건너뛴다.
        if (activeProfile.equals("dev", ignoreCase = true)) {
            logger().info(
                "Discord 신규 가입 알림 건너뜀 (dev 프로파일): userId={}, provider={}",
                event.userId,
                event.oAuthType,
            )
            return
        }
        try {
            val count = userRepository.count()
            val phase = activeProfile.uppercase()
            val embed = buildEmbed(event, count, phase)
            discordHookApi.sendMessage(DiscordHookApi.Request(embeds = listOf(embed)))
        } catch (e: Exception) {
            logger().error(
                "Discord 신규 가입 알림 발송 실패: userId={}, provider={}",
                event.userId,
                event.oAuthType,
                e,
            )
        }
    }

    private fun buildEmbed(
        event: UserRegisteredEvent,
        count: Long,
        phase: String,
    ): DiscordHookApi.Embed {
        val providerEmoji = providerEmoji(event.oAuthType)
        val color = providerColor(event.oAuthType)
        val phaseEmoji = phaseEmoji(phase)
        val registeredAtKst = "${KST_FORMATTER.format(event.registeredAt)} KST"
        val countFormatted = "%,d".format(count)

        return DiscordHookApi.Embed(
            title = "$providerEmoji 신규 가입",
            description = milestoneText(count),
            color = color,
            fields =
                listOf(
                    DiscordHookApi.Field(name = "닉네임", value = event.nickName, inline = true),
                    DiscordHookApi.Field(name = "Provider", value = event.oAuthType.name, inline = true),
                    DiscordHookApi.Field(name = "User ID", value = "`${event.userId}`", inline = false),
                    DiscordHookApi.Field(name = "가입 시각", value = registeredAtKst, inline = true),
                    DiscordHookApi.Field(name = "누적 가입자", value = "${countFormatted}명", inline = true),
                ),
            footer = DiscordHookApi.Footer(text = "$phaseEmoji $phase · ddan-ddan-server"),
            timestamp = event.registeredAt.toString(),
        )
    }

    private fun providerEmoji(type: OAuthType): String =
        when (type) {
            OAuthType.KAKAO -> "🍫"
            OAuthType.APPLE -> "🍎"
        }

    private fun providerColor(type: OAuthType): Int =
        when (type) {
            OAuthType.KAKAO -> 0xFEE500
            OAuthType.APPLE -> 0x1C1C1E
        }

    private fun phaseEmoji(phase: String): String =
        when (phase) {
            "PROD" -> "🚀"
            "DEV" -> "🧪"
            else -> "💻"
        }

    private fun milestoneText(count: Long): String? =
        when {
            count <= 0L -> null
            count % 1000L == 0L -> "🎉 ${"%,d".format(count)}명 달성!"
            count % 500L == 0L -> "✨ ${"%,d".format(count)}명 달성!"
            count % 100L == 0L -> "🎯 ${"%,d".format(count)}명 달성!"
            else -> null
        }

    companion object {
        private val KST_FORMATTER: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Seoul"))
    }
}
