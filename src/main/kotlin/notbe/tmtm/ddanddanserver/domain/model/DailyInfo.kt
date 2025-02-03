package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateObjectId
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import java.time.LocalDate
import kotlin.math.max

class DailyInfo(
    val id: String,
    val userId: String,
    var userName: String?,
    var petType: PetType?,
    val date: LocalDate,
    var calorie: Int,
    var purposeAchieved: Boolean = false,
    var toyGiven: Boolean = false,
) {
    fun update(calorie: Int) {
        this.calorie = max(this.calorie, calorie)
    }

    companion object {
        fun create(
            userId: String,
            userName: String?,
            petType: PetType?,
            date: LocalDate = LocalDate.now(),
            calorie: Int = 0,
        ): DailyInfo =
            DailyInfo(
                id = generateObjectId(),
                userId = userId,
                userName = userName,
                petType = petType,
                date = date,
                calorie = calorie,
            )
    }
}
