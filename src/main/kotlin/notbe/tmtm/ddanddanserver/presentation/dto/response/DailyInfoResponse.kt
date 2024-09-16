package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import java.time.LocalDate

@Schema(description = "일일 정보 응답 DTO")
data class DailyInfoResponse(
    @Schema(description = "일일 정보 식별 ID", example = "ABCDEF1234567")
    val id: String,
    @Schema(description = "유저 식별 ID", example = "ABCDEF1234567")
    val userId: String,
    @Schema(description = "일자", example = "2021-01-01")
    val date: LocalDate,
    @Schema(description = "일일 칼로리", example = "100")
    val calorie: Int,
) {
    companion object {
        fun fromDomain(dailyInfo: DailyInfo) =
            with(dailyInfo) {
                DailyInfoResponse(
                    id = id,
                    userId = userId,
                    date = date,
                    calorie = calorie,
                )
            }
    }
}
