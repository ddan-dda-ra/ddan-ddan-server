package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "칼로리 요청 DTO")
data class CalorieRequest(
    @Schema(description = "칼로리", example = "100", minimum = "0", maximum = "10000")
    val calorie: Int,
) {
    init {
        // 100kcal마다 먹이를 지급하므로, 비정상적으로 큰 값으로 먹이를 대량으로 받지 못하게 막는다.
        require(calorie in MIN_CALORIE..MAX_CALORIE) { "calorie should be between $MIN_CALORIE and $MAX_CALORIE" }
    }

    companion object {
        const val MIN_CALORIE = 0
        const val MAX_CALORIE = 10_000
    }
}
