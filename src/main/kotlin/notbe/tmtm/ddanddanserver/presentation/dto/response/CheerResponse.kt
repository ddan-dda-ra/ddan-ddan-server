package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "응원하기 응답 DTO")
data class CheerResponse(
    @Schema(description = "응원 ID", example = "64a1b2c3d4e5f6789012345")
    val cheerId: String,
    @Schema(description = "응원받은 사용자 ID", example = "64a1b2c3d4e5f6789012346")
    val cheereeId: String,
    @Schema(description = "응원한 날짜", example = "2024-08-23")
    val date: LocalDate,
    @Schema(description = "응원한 시각", example = "2024-08-23T10:30:00")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(cheer: Cheer): CheerResponse =
            CheerResponse(
                cheerId = cheer.id.toString(),
                cheereeId = cheer.cheereeId.toString(),
                date = cheer.date,
                createdAt = cheer.createdAt,
            )
    }
}