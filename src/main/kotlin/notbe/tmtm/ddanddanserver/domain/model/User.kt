package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateTsid

class User(
    val id: String,
    var name: String?,
    var purposeCalorie: Int = 100,
    var foodQuantity: Int = 0,
    var toyQuantity: Int = 0,
) {
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
