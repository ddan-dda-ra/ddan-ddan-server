package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import notbe.tmtm.ddanddanserver.domain.model.User

@Entity(name = "users")
data class UserEntity(
    @Id
    @Column(name = "id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val id: String,
    @Column(name = "name", nullable = true)
    val name: String?,
    @Column(name = "purpose_calorie", nullable = false)
    val purposeCalorie: Int,
    @Column(name = "food_quantity", nullable = false)
    val foodQuantity: Int,
    @Column(name = "toy_quantity", nullable = false)
    val toyQuantity: Int,
) : BaseEntity() {
    fun toDomain() =
        User(
            id = id,
            name = name,
            purposeCalorie = purposeCalorie,
            foodQuantity = foodQuantity,
            toyQuantity = toyQuantity,
        )

    companion object {
        fun fromDomain(user: User) =
            with(user) {
                UserEntity(
                    id = id,
                    name = name,
                    purposeCalorie = purposeCalorie,
                    foodQuantity = foodQuantity,
                    toyQuantity = toyQuantity,
                )
            }
    }
}
