package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.ranking.*

@Schema(description = "랭킹 조회 응답 DTO")
data class RankingResponse(
    @Schema(description = "랭킹 조회 기준", example = "TOTAL_CALORIES, TOTAL_SUCCEEDED_DAYS")
    val criteria: RankingCriteria,
    @Schema(description = "랭킹 조회 기간", example = "DAILY, WEEKLY, MONTHLY, YEARLY")
    val periodType: PeriodType,
    @Schema(description = "랭킹 조회 결과")
    val ranking: List<RankingUserStatResponse>,
    @Schema(description = "내 랭킹 정보")
    val myRanking: RankingUserStatResponse,
) {
    @Schema(description = "유저 랭킹 정보 DTO")
    data class RankingUserStatResponse(
        @Schema(description = "랭킹", example = "1")
        val rank: Int,
        @Schema(description = "유저 ID", example = "userid")
        val userId: String,
        @Schema(description = "유저 이름", example = "홍길동")
        val userName: String,
        @Schema(description = "주 펫 종류 (PetCatalog.type)", example = "DOG")
        val mainPetType: String,
        @Schema(description = "펫 레벨", example = "1")
        val petLevel: Int = 5,
        @Schema(description = "총 칼로리", example = "1000")
        val totalCalories: Int,
        @Schema(description = "총 성공 일수", example = "10")
        val totalSucceededDays: Int,
    ) {
        companion object {
            fun fromDomain(
                rank: Int,
                userStat: UserStat,
            ) = RankingUserStatResponse(
                rank = rank,
                userId = userStat.user.id.toHexString(),
                userName = userStat.user.name!!,
                mainPetType = userStat.mainPet.type,
                petLevel = userStat.mainPet.getLevel(),
                totalCalories = userStat.totalCalories,
                totalSucceededDays = userStat.totalSucceededDays,
            )

            fun fromDomain(userRanking: UserRanking) = fromDomain(
                rank = userRanking.rank,
                userStat = userRanking.userStat,
            )
        }
    }

    companion object {
        fun fromDomain(
            criteria: RankingCriteria,
            periodType: PeriodType,
            result: RankingResult,
        ) = RankingResponse(
            criteria = criteria,
            periodType = periodType,
            ranking = result.getTopRankings(100).map { userRanking -> RankingUserStatResponse.fromDomain(userRanking) },
            myRanking = RankingUserStatResponse.fromDomain(result.myRanking),
        )
    }
}
