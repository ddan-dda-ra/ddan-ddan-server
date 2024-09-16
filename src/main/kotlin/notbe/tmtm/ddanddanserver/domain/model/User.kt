package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateTsid
import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException

class User(
    val id: String,
    var name: String?,
    var purposeCalorie: Int = 100,
    var foodQuantity: Int = 0,
    var toyQuantity: Int = 0,
) {
    fun feed(quantity: Int = 1) {
        if (this.foodQuantity < quantity) {
            throw UserFoodQuantityLackException("먹이가 부족합니다")
        }
        this.foodQuantity -= quantity
    }

    fun update(
        name: String,
        purposeCalorie: Int,
    ) {
        this.name = name
        this.purposeCalorie = purposeCalorie
    }

    companion object {
        fun register(name: String? = null): User = User(id = generateTsid(), name = name)
    }
}
