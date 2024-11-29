package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "탈퇴 요청 DTO")
data class WithDrawRequest(
    @Schema(description = "탈퇴 사유", example = "사유")
    val cause: String,
)
