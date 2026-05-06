package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "펫 추가 요청 DTO")
data class AddPetRequest(
    @Schema(description = "펫 종류 (PetCatalog.type)", example = "DOG")
    @field:NotBlank
    val petType: String,
)
