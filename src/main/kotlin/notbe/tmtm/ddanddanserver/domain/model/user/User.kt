package notbe.tmtm.ddanddanserver.domain.model.user

import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserToyQuantityLackException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("users")
class User(
    val id: ObjectId,
    var deviceToken: String?,
    var name: String?,
    var mainPetId: ObjectId? = null,
    var purposeCalorie: Int = 100,
    var foodQuantity: Int = 0,
    var toyQuantity: Int = 0,
    var setting: UserSetting = UserSetting(),
    var lastLoginAt: LocalDate = LocalDate.now(),
) {
    fun feed(quantity: Int = 1) {
        if (this.foodQuantity < quantity) {
            throw UserFoodQuantityLackException()
        }
        this.foodQuantity -= quantity
    }

    fun play(quantity: Int = 1) {
        if (this.toyQuantity < quantity) {
            throw UserToyQuantityLackException()
        }
        this.toyQuantity -= quantity
    }

    fun update(
        name: String,
        purposeCalorie: Int,
    ) {
        this.name = name
        this.purposeCalorie = purposeCalorie
    }

    fun updateDeviceToken(deviceToken: String) {
        this.deviceToken = deviceToken
    }

    fun setMainPet(petId: ObjectId) {
        this.mainPetId = petId
    }

    fun hasMainPet(): Boolean = this.mainPetId != null

    companion object {
        fun register(
            deviceToken: String?,
            name: String? = null,
        ): User = User(id = ObjectId(), deviceToken = deviceToken, name = name)
    }
}
