package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.RankingBoardRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.deleteAllBy
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.max

@Service
class UserService(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val dailyInfoRepository: DailyInfoRepository,
    private val petRepository: PetRepository,
    private val inviteCodeRepository: InviteCodeRepository,
    private val friendshipRepository: FriendshipRepository,
    private val cheerRepository: CheerRepository,
    private val rankingBoardRepository: RankingBoardRepository,
) {
    private val log = logger()

    fun getAll(): List<User> = userRepository.findAll()

    fun getAllByIds(ids: List<ObjectId>): List<User> = userRepository.findAllById(ids)

    fun getByIdOrThrow(id: ObjectId): User = userRepository.findByIdOrThrow(id)

    fun updateInfo(
        userId: ObjectId,
        name: String,
        purposeCalorie: Int,
    ): User {
        val user = getByIdOrThrow(userId)
        user.updateInfo(name, purposeCalorie)
        return userRepository.updateProfile(userId, user.name, user.purposeCalorie)
    }

    fun updateSetting(
        userId: ObjectId,
        isAppPushOn: Boolean?,
    ): UserSetting {
        val user = getByIdOrThrow(userId)
        val setting =
            user.setting.copy(
                isAppPushOn = isAppPushOn ?: user.setting.isAppPushOn,
            )
        return userRepository.updateSetting(userId, setting).setting
    }

    fun resetPurposeStrictExcept(userIds: Collection<ObjectId>): Long = userRepository.resetPurposeStrictExcept(userIds)

    fun withdraw(userId: ObjectId) {
        authRepository.deleteAllByUserId(userId)
        dailyInfoRepository.deleteAllByUserId(userId)
        friendshipRepository.deleteAllBy(userId)
        inviteCodeRepository.deleteAllByInviterId(userId)
        petRepository.deleteAllByOwnerUserId(userId)
        cheerRepository.deleteAllByCheererIdOrCheereeId(userId, userId)
        rankingBoardRepository.removeUser(userId)

        userRepository.deleteById(userId)
    }

    data class CalorieUpdateResult(
        val user: User,
        val dailyInfo: DailyInfo,
        val rewardedFoodQuantity: Int,
        val rewardedToyQuantity: Int,
    )

    /**
     * 칼로리 갱신과 보상 지급을 원자적 연산으로 나눠 처리한다.
     * 1. DailyInfo 칼로리를 $max로 올리고 갱신 전 칼로리를 받는다. 동시 요청이 와도 각 요청이 받는 이전 값은 겹치지 않아 보상이 중복되지 않는다.
     * 2. 목표 달성은 아직 달성 처리 전인 문서만 바꾸는 조건부 갱신이라 하루에 한 번만 인정된다.
     * 3. 먹이·연속 달성·접속일을 한 번에 $inc/$set 한다.
     * 3이 실패하면 1, 2를 되돌린다. 클라이언트가 같은 칼로리로 다시 요청하면 보상을 다시 계산해서 받는다.
     * $inc는 멱등이 아니라서 서버에서 자동 재시도하지 않는다. 첫 시도가 실제로는 반영됐다면 보상이 두 번 들어간다.
     */
    @Transactional
    fun updateCalorieAndRewardFood(
        userId: ObjectId,
        calorie: Int,
        today: LocalDate,
    ): CalorieUpdateResult {
        val user = getByIdOrThrow(userId)
        val dailyInfo = getOrCreateDailyInfo(user, today)
        val previousCalorie = dailyInfoRepository.raiseCalorie(dailyInfo.id, calorie)
        val rewardFood = getRewardFood(previousCalorie, calorie)

        var purposeAchieved = false
        val rewardedUser =
            try {
                purposeAchieved = isDailyPurposeAchieve(previousCalorie, user.purposeCalorie, calorie) &&
                    dailyInfoRepository.markPurposeAchieved(dailyInfo.id)
                userRepository.rewardDailyCalorie(userId, rewardFood, purposeAchieved, LocalDate.now())
            } catch (e: Exception) {
                revertDailyCalorie(dailyInfo.id, previousCalorie, calorie, purposeAchieved)
                throw e
            }

        val (resultUser, rewardedToyQuantity) =
            if (purposeAchieved && validateToyGiven(rewardedUser.purposeStrict)) {
                rewardToy(rewardedUser, dailyInfo.id)
            } else {
                rewardedUser to 0
            }

        return CalorieUpdateResult(
            resultUser,
            dailyInfoRepository.findByIdOrNull(dailyInfo.id) ?: dailyInfo,
            rewardFood,
            rewardedToyQuantity,
        )
    }

    private fun revertDailyCalorie(
        dailyInfoId: ObjectId,
        previousCalorie: Int,
        calorie: Int,
        purposeAchieved: Boolean,
    ) {
        runCatching {
            if (calorie > previousCalorie) {
                dailyInfoRepository.revertCalorie(dailyInfoId, from = calorie, to = previousCalorie)
            }
            if (purposeAchieved) {
                dailyInfoRepository.unmarkPurposeAchieved(dailyInfoId)
            }
        }.onFailure {
            log.error("칼로리 갱신 되돌리기 실패. dailyInfoId: $dailyInfoId, $previousCalorie -> $calorie", it)
        }
    }

    /**
     * 먹이 보상과 연속 달성은 이미 반영됐으므로, 장난감 지급이 실패해도 요청은 성공으로 응답한다.
     */
    private fun rewardToy(
        user: User,
        dailyInfoId: ObjectId,
    ): Pair<User, Int> =
        runCatching {
            val rewardedUser = userRepository.increaseItems(user.id, toy = 1)
            dailyInfoRepository.markToyGiven(dailyInfoId)
            rewardedUser to 1
        }.getOrElse {
            log.error("연속 달성 장난감 지급 실패. userId: ${user.id}, dailyInfoId: $dailyInfoId", it)
            user to 0
        }

    private fun getOrCreateDailyInfo(
        user: User,
        today: LocalDate,
    ): DailyInfo {
        dailyInfoRepository.findByUserIdAndDate(user.id, today)?.let { return it }

        val mainPetType = user.mainPetId?.let { petRepository.findByIdOrNull(it)?.type }
        return try {
            if (dailyInfoRepository.findByUserIdAndDate(user.id, today.minusDays(1))?.purposeAchieved == false) {
                userRepository.resetPurposeStrict(user.id)
            }
            dailyInfoRepository.insert(DailyInfo.create(user.id, user.name, mainPetType, today))
        } catch (e: DuplicateKeyException) {
            dailyInfoRepository.findByUserIdAndDate(user.id, today)!!
        }
    }

    private fun validateToyGiven(purposeStrict: Int): Boolean = purposeStrict != 0 && purposeStrict % 3 == 0

    private fun isDailyPurposeAchieve(
        previousCalorie: Int,
        purposeCalorie: Int,
        currentCalorie: Int,
    ) = previousCalorie < purposeCalorie && currentCalorie >= purposeCalorie

    private fun getRewardFood(
        previousCalorie: Int,
        currentCalorie: Int,
    ): Int = max(currentCalorie / CALORIE_REWARD_UNIT - previousCalorie / CALORIE_REWARD_UNIT, 0)

    companion object {
        private const val CALORIE_REWARD_UNIT = 100
    }
}
