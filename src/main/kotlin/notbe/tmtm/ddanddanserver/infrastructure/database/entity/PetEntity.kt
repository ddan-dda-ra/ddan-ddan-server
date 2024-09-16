package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import jakarta.persistence.*
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType

@Entity(name = "pets")
data class PetEntity(
    @Id
    @Column(name = "id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val id: String,
    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: PetType,
    @Column(name = "owner_user_id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val ownerUserId: String,
    @Column(name = "exp", nullable = false)
    val exp: Int,
) : BaseEntity() {
    fun toDomain() =
        Pet(
            id = id,
            type = type,
            ownerUserId = ownerUserId,
            exp = exp,
        )

    companion object {
        fun fromDomain(pet: Pet) =
            with(pet) {
                PetEntity(
                    id = id,
                    type = type,
                    ownerUserId = ownerUserId,
                    exp = exp,
                )
            }
    }
}
