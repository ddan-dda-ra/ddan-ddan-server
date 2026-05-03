package notbe.tmtm.ddanddanserver.infrastructure.api

import com.fasterxml.jackson.annotation.JsonInclude
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Request(
        val username: String = DEFAULT_USERNAME,
        val content: String? = null,
        val embeds: List<Embed>? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Embed(
        val title: String? = null,
        val description: String? = null,
        val color: Int? = null,
        val fields: List<Field>? = null,
        val footer: Footer? = null,
        val timestamp: String? = null,
    )

    data class Field(
        val name: String,
        val value: String,
        val inline: Boolean = false,
    )

    data class Footer(
        val text: String,
    )

    companion object {
        const val DEFAULT_USERNAME = "ddan-ddan-server-bot"
    }
}
