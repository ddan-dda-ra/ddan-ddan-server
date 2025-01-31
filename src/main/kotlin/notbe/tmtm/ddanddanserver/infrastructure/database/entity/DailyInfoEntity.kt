package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("daily_calories")
data class DailyInfoEntity(
    @Id
    val id: String,
    val userId: String,
    val calorie: Int,
    val purposeAchieved: Boolean = false,
    val toyGiven: Boolean = false,
    val date: LocalDate = LocalDate.now(),
) {
    fun toDomain() =
        DailyInfo(
            id = id,
            userId = userId,
            date = date,
            calorie = calorie,
            purposeAchieved = purposeAchieved,
            toyGiven = toyGiven,
        )

    companion object {
        fun fromDomain(dailyInfo: DailyInfo) =
            with(dailyInfo) {
                DailyInfoEntity(
                    id = id,
                    userId = userId,
                    calorie = calorie,
                    purposeAchieved = purposeAchieved,
                    toyGiven = toyGiven,
                )
            }
    }
}
