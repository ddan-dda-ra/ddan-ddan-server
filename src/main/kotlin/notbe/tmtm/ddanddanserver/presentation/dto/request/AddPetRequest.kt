package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType

@Schema(description = "펫 추가 요청 DTO")
data class AddPetRequest(
    @Schema(description = "펫 종류", example = "DOG")
    val petType: PetType,
)
