package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType

@Schema(description = "펫 정보 응답 DTO")
data class PetResponse(
    @Schema(description = "Pet 식별 ID", example = "ABCDEF1234567")
    val id: String,
    @Schema(description = "Pet 종류", example = "DOG")
    val type: PetType,
    @Schema(description = "Pet 레벨", example = "1")
    val level: Int,
    @Schema(description = "현재 레벨에 해당하는 경험치 퍼센트", example = "15.5")
    val expPercent: Double,
) {
    companion object {
        fun fromDomain(userPet: Pet) =
            with(userPet) {
                PetResponse(
                    id = id,
                    type = type,
                    level = getLevel().level,
                    expPercent = getExpPercent(),
                )
            }
    }
}
