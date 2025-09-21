package notbe.tmtm.ddanddanserver.presentation.dto.response

import notbe.tmtm.ddanddanserver.domain.model.user.UserProfile
import org.bson.types.ObjectId

data class UserProfileResponse(
    val userId: String,
    val userName: String,
    val mainPet: PetResponse,
    val todayCalorie: Int,
    val monthlyReceivedCheerCount: Int,
    val isFriend: Boolean,
    val isCheeredToday: Boolean,
) {
    companion object {
        fun fromDomain(userProfile: UserProfile, myId: ObjectId): UserProfileResponse =
            UserProfileResponse(
                userId = userProfile.user.id.toHexString(),
                userName = userProfile.user.name ?: "",
                mainPet = PetResponse.fromDomain(userProfile.mainPet),
                todayCalorie = userProfile.todayDailyInfo.calorie,
                monthlyReceivedCheerCount = userProfile.receivedCheers.size,
                isFriend = userProfile.isFriend,
                isCheeredToday = userProfile.isCheeredToday(myId)
            )
    }
}
