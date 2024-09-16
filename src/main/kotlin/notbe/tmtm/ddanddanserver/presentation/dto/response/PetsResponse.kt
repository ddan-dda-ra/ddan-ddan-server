package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

@Schema(description = "유저가 소유한 펫 응답 DTO")
data class PetsResponse(
    @Schema(description = "Pet 소유 User 식별 ID")
    val ownerUserId: String,
    @Schema(description = "Pets")
    val pets: List<PetResponse>,
) {
    companion object {
        fun fromDomain(
            ownerUserId: String,
            pets: List<Pet>,
        ) = PetsResponse(
            ownerUserId = ownerUserId,
            pets = pets.map { PetResponse.fromDomain(it) },
        )
    }
}
