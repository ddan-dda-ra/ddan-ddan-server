package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("pets")
data class PetEntity(
    @Id
    val id: String,
    val type: PetType,
    val ownerUserId: String,
    val exp: Int,
) {
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
