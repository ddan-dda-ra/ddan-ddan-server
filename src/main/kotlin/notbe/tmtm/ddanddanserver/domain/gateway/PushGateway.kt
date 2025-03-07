package notbe.tmtm.ddanddanserver.domain.gateway

interface PushGateway {
    fun send(
        deviceToken: String,
        content: String,
    )

    fun sendAll(
        deviceTokens: List<String>,
        content: String,
    )
}
