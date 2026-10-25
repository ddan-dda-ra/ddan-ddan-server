package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import org.bson.types.ObjectId
import java.time.LocalDate

/**
 * User 문서를 필드 단위로 원자적으로 갱신한다.
 * 문서 전체 save()는 동시 요청의 변경을 덮어쓰므로, 생성 외에는 이 메서드들로만 갱신한다.
 * 갱신 결과를 반환하는 메서드는 갱신 후 문서를 반환한다.
 */
interface UserRepositoryCustom {
    fun increaseTickets(userId: ObjectId, amount: Int)

    fun decreaseTickets(userId: ObjectId, amount: Int)

    fun consumeFood(userId: ObjectId, quantity: Int): User

    fun consumeToy(userId: ObjectId, quantity: Int): User

    fun increaseItems(userId: ObjectId, food: Int = 0, toy: Int = 0, tickets: Int = 0): User

    fun rewardDailyCalorie(userId: ObjectId, food: Int, purposeAchieved: Boolean, lastLoginAt: LocalDate): User

    fun resetPurposeStrict(userId: ObjectId)

    fun resetPurposeStrictExcept(userIds: Collection<ObjectId>): Long

    fun updateProfile(userId: ObjectId, name: String?, purposeCalorie: Int): User

    fun updateSetting(userId: ObjectId, setting: UserSetting): User

    fun updateMainPetId(userId: ObjectId, mainPetId: ObjectId): User

    fun updateDeviceToken(userId: ObjectId, deviceToken: DeviceToken?)
}
