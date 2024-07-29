package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.exception.ErrorCode

@Schema(description = "에러 응답 DTO")
data class ErrorResponse(
    @Schema(description = "에러 응답 코드")
    val code: String,
    @Schema(description = "에러 응답 메시지")
    val message: String,
    @Schema(description = "추가 데이터")
    val data: Any? = null,
) {
    companion object {
        fun fromErrorCode(
            errorCode: ErrorCode,
            data: Any? = null,
        ) = with(errorCode) {
            ErrorResponse(
                code = code,
                message = message,
                data = data,
            )
        }
    }
}
