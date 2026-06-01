package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User

data class UserStat(
    val user: User,
    val mainPet: Pet,
    val totalCalories: Int,
    val totalSucceededDays: Int,
    val totalAttendanceDays: Int,
)
