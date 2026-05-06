package notbe.tmtm.ddanddanserver.presentation.dto.admin

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo

data class DailyCaloriesAdminListResponse(
    val userId: String,
    val from: String,
    val to: String,
    val records: List<DailyCaloriesAdminItemResponse>,
) {
    companion object {
        fun from(
            userId: String,
            from: String,
            to: String,
            records: List<DailyInfo>,
        ): DailyCaloriesAdminListResponse =
            DailyCaloriesAdminListResponse(
                userId = userId,
                from = from,
                to = to,
                records = records.map { DailyCaloriesAdminItemResponse.from(it) },
            )
    }
}

data class DailyCaloriesAdminItemResponse(
    val date: String,
    val calorie: Int,
    val purposeAchieved: Boolean,
    val toyGiven: Boolean,
    val petType: String?,
) {
    companion object {
        fun from(info: DailyInfo): DailyCaloriesAdminItemResponse =
            DailyCaloriesAdminItemResponse(
                date = info.date.toString(),
                calorie = info.calorie,
                purposeAchieved = info.purposeAchieved,
                toyGiven = info.toyGiven,
                petType = info.petType,
            )
    }
}
