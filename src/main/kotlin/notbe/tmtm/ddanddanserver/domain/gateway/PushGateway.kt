package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView

interface PushGateway {
    fun send(
        deviceToken: String,
        content: String,
        routingView: RoutingView,
    )

    fun sendAll(
        deviceTokens: List<String>,
        content: String,
        routingView: RoutingView,
    )
}
