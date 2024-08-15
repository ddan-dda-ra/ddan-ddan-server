package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Token Refresh 요청 DTO")
data class RefreshTokenRequest(
    @Schema(description = "리프레시 토큰", example = "REFRESH_TOKEN")
    val refreshToken: String,
)
