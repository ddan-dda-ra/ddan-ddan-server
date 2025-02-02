package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.User
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class UserEntity(
    @Id
    val id: String,
    val deviceToken: String,
    val name: String?,
    val mainPetId: String?,
    val purposeCalorie: Int,
    val foodQuantity: Int,
    val toyQuantity: Int,
) {
    fun toDomain() =
        User(
            id = id,
            deviceToken = deviceToken,
            name = name,
            mainPetId = mainPetId,
            purposeCalorie = purposeCalorie,
            foodQuantity = foodQuantity,
            toyQuantity = toyQuantity,
        )

    companion object {
        fun fromDomain(user: User) =
            with(user) {
                UserEntity(
                    id = id,
                    deviceToken = deviceToken,
                    name = name,
                    mainPetId = mainPetId,
                    purposeCalorie = purposeCalorie,
                    foodQuantity = foodQuantity,
                    toyQuantity = toyQuantity,
                )
            }
    }
}
