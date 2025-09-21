package notbe.tmtm.ddanddanserver.domain.model.user

import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId

data class UserProfile(
    val user: User,
    val mainPet: Pet,
    val todayDailyInfo: DailyInfo,
    val receivedCheers: List<Cheer>,
    val isFriend: Boolean,
) {
    fun isCheeredToday(userId: ObjectId): Boolean {
        return receivedCheers.any { it.cheererId == userId && it.date.isEqual(todayDailyInfo.date) }
    }
}
