package notbe.tmtm.ddanddanserver.domain.model.auth

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document("auths")
class Auth(
    val id: ObjectId,
    val oAuthId: String,
    val type: OAuthType,
    val userId: ObjectId,
) {
    companion object {
        fun create(
            oAuthId: String,
            type: OAuthType,
            userId: ObjectId,
        ): Auth =
            Auth(
                id = ObjectId(),
                oAuthId = oAuthId,
                type = type,
                userId = userId,
            )
    }
}
