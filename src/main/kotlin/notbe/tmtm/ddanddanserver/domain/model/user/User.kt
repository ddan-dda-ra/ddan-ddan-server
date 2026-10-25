package notbe.tmtm.ddanddanserver.domain.model.user

import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("users")
class User(
    val id: ObjectId,
    var deviceToken: DeviceToken?,
    var name: String?,
    var mainPetId: ObjectId? = null,
    var purposeCalorie: Int = 100,
    var foodQuantity: Int = 0,
    var toyQuantity: Int = 0,
    var purposeStrict: Int = 0,
    var tickets: Int = 0,
    var setting: UserSetting = UserSetting(),
    var lastLoginAt: LocalDate = LocalDate.now(),
) {
    fun updateInfo(
        name: String,
        purposeCalorie: Int,
    ) {
        require(purposeCalorie in 100..1000) { "purposeCalorie should be between 100 and 1000" }
        this.name = name
        this.purposeCalorie = purposeCalorie
    }

    fun updateDeviceToken(deviceToken: String) {
        this.deviceToken = deviceToken.toDeviceToken()
    }

    fun hasMainPet(): Boolean = this.mainPetId != null

    fun getMainPetIdOrThrow(): ObjectId = this.mainPetId ?: throw PetNotFoundException("Main pet is not set")

    companion object {
        fun register(
            deviceToken: String?,
            name: String? = null,
        ): User = User(id = ObjectId(), deviceToken = deviceToken.toDeviceToken(), name = name)
    }
}
