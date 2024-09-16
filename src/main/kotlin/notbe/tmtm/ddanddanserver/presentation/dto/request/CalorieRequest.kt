package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "칼로리 요청 DTO")
data class CalorieRequest(
    @Schema(description = "칼로리", example = "100")
    val calorie: Int,
)
