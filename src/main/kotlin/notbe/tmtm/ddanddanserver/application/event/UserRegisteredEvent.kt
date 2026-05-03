package notbe.tmtm.ddanddanserver.application.event

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.bson.types.ObjectId
import java.time.Instant

data class UserRegisteredEvent(
    val userId: ObjectId,
    val nickName: String,
    val oAuthType: OAuthType,
    val registeredAt: Instant,
)
