package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(accept = [MediaType.APPLICATION_JSON_VALUE])
interface SlackHookApi {
    @PostExchange
    fun sendMessage(
        @RequestBody request: Request,
    )

    data class Request(
        val channel: String = SERVER_ALERT_CHANNEL,
        val text: String,
        val username: String = "ddan-ddan-server-bot",
    )

    companion object {
        const val SERVER_ALERT_CHANNEL = "#server-github-channel"
        const val WITHDRAW_ALERT_CHANNEL = "#withdrawer-channel"
    }
}
