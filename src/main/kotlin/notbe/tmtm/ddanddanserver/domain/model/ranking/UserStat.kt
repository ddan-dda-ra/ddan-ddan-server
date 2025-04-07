package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.bson.types.ObjectId

data class UserStat(
    val userId: ObjectId,
    val userName: String,
    val mainPetType: PetType,
    val totalCalories: Int,
    val totalSucceededDays: Int,
)
