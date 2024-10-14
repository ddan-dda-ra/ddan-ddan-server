package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateTsid
import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserToyQuantityLackException

class User(
    val id: String,
    var name: String?,
    var mainPetId: String? = null,
    var purposeCalorie: Int = 100,
    var foodQuantity: Int = 0,
    var toyQuantity: Int = 0,
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

    fun setMainPet(petId: String) {
        this.mainPetId = petId
    }

    fun hasMainPet(): Boolean = this.mainPetId != null

    companion object {
        fun register(name: String? = null): User = User(id = generateTsid(), name = name)
    }
}
