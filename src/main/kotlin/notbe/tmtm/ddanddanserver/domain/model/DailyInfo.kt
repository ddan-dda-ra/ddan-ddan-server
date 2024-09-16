package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateTsid
import java.time.LocalDate
import kotlin.math.max

class DailyInfo(
    val id: String,
    val userId: String,
    val date: LocalDate,
    var calorie: Int,
) {
    fun update(calorie: Int) {
        this.calorie = max(this.calorie, calorie)
    }

    companion object {
        fun register(
            userId: String,
            date: LocalDate = LocalDate.now(),
            calorie: Int = 0,
        ): DailyInfo = DailyInfo(id = generateTsid(), userId = userId, date = date, calorie = calorie)
    }
}
