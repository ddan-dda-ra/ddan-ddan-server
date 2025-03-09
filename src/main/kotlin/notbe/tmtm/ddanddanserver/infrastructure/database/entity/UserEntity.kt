package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

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
    val setting: UserSetting = UserSetting(),
    val lastLoginAt: LocalDate = LocalDate.now(),
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
            setting = setting,
            lastLoginAt = lastLoginAt,
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
                    setting = setting,
                    lastLoginAt = lastLoginAt,
                )
            }
    }
}
