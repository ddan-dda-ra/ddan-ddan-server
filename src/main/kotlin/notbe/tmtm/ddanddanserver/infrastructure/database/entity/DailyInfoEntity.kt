package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("daily_calories")
data class DailyInfoEntity(
    @Id
    val id: String,
    val userId: String,
    val userName: String?,
    val petType: PetType?,
    val calorie: Int,
    val purposeAchieved: Boolean = false,
    val toyGiven: Boolean = false,
    val date: LocalDate = LocalDate.now(),
) {
    fun toDomain() =
        DailyInfo(
            id = id,
            userId = userId,
            userName = userName,
            petType = petType,
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
                    userName = userName,
                    petType = petType,
                    calorie = calorie,
                    purposeAchieved = purposeAchieved,
                    toyGiven = toyGiven,
                )
            }
    }
}
