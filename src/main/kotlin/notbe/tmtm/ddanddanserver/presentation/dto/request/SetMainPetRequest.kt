package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "메인 펫 설정 요청 DTO")
data class SetMainPetRequest(
    @Schema(description = "펫 ID", example = "1234567890")
    val petId: String,
)
