package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

@Schema(description = "메인 펫 정보 응답 DTO")
data class UserMainPetResponse(
    @Schema(description = "메인 펫 정보")
    val mainPet: PetResponse?,
) {
    companion object {
        fun fromDomain(mainPet: Pet?) =
            UserMainPetResponse(
                mainPet = mainPet?.let { PetResponse.fromDomain(it) },
            )
    }
}
