package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView

interface PushClient {
    fun sendToMultiple(deviceTokens: List<String>, message: String, routingView: RoutingView)
    fun sendToUser(deviceToken: String, message: String, routingView: RoutingView)
}
