package notbe.tmtm.ddanddanserver.domain.model.user

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import kotlin.math.max

@Document("daily_calories")
class DailyInfo(
    val id: ObjectId,
    val userId: ObjectId,
    var userName: String?,
    var petType: PetType?,
    var calorie: Int,
    var purposeAchieved: Boolean = false,
    var toyGiven: Boolean = false,
    val date: LocalDate,
) {
    fun update(calorie: Int) {
        this.calorie = max(this.calorie, calorie)
    }

    companion object {
        fun create(
            userId: ObjectId,
            userName: String?,
            petType: PetType?,
            date: LocalDate = LocalDate.now(),
            calorie: Int = 0,
        ): DailyInfo =
            DailyInfo(
                id = ObjectId(),
                userId = userId,
                userName = userName,
                petType = petType,
                date = date,
                calorie = calorie,
            )
    }
}
