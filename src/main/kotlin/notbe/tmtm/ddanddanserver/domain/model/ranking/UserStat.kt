package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType

data class UserStat(
    val userId: String,
    val userName: String,
    val mainPetType: PetType,
    val totalCalories: Int,
    val totalSucceededDays: Int,
)
