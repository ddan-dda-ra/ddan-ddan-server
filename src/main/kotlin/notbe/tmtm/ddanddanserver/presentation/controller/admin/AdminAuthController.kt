package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import notbe.tmtm.ddanddanserver.application.service.AdminAuthService
import notbe.tmtm.ddanddanserver.presentation.dto.admin.AdminLoginRequest
import notbe.tmtm.ddanddanserver.presentation.dto.admin.AdminLoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/auth")
@Tag(name = "Admin · 인증", description = "운영자 로그인")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
) {
    @Operation(summary = "운영자 로그인", description = "ID/PW 검증 후 어드민 access token 발급 (1일 만료)")
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: AdminLoginRequest,
    ): AdminLoginResponse =
        AdminLoginResponse(
            accessToken = adminAuthService.login(request.username, request.password),
        )
}
