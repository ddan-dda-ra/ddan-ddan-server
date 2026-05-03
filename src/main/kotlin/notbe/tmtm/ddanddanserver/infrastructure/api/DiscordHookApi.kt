package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(accept = [MediaType.APPLICATION_JSON_VALUE])
interface DiscordHookApi {
    @PostExchange
    fun sendMessage(
        @RequestBody request: Request,
    )

    data class Request(
        val content: String,
        val username: String = DEFAULT_USERNAME,
    )

    companion object {
        const val DEFAULT_USERNAME = "ddan-ddan-server-bot"
    }
}
