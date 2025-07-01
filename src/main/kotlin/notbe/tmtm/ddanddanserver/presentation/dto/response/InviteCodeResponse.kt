package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.friend.InviteCode
import java.time.LocalDateTime

@Schema(description = "초대코드 응답 DTO")
data class InviteCodeResponse(
    @Schema(description = "초대코드", example = "ABC12345")
    val code: String,
    @Schema(description = "만료 시간", example = "2024-07-02T15:30:00")
    val expiresAt: LocalDateTime,
    @Schema(description = "생성 시간", example = "2024-07-01T15:30:00")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(inviteCode: InviteCode): InviteCodeResponse =
            InviteCodeResponse(
                code = inviteCode.getCode(),
                expiresAt = inviteCode.expiresAt,
                createdAt = inviteCode.createdAt,
            )
    }
}
