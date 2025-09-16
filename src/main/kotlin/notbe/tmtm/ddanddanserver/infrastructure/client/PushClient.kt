package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken

interface PushClient {
    fun sendToMultiple(deviceTokens: List<DeviceToken?>, message: String, routingView: RoutingView)
    fun sendToUser(deviceToken: DeviceToken?, message: String, routingView: RoutingView)
}
