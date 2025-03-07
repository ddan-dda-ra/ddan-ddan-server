package notbe.tmtm.ddanddanserver.domain.gateway

interface PushGateway {
    fun send(
        token: String,
        content: String,
    )

    fun sendAll(
        tokens: List<String>,
        content: String,
    )
}
