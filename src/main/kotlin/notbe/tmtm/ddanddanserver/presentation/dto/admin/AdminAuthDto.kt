package notbe.tmtm.ddanddanserver.presentation.dto.admin

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

data class AdminLoginResponse(
    val accessToken: String,
)
