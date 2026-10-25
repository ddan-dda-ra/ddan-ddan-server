package notbe.tmtm.ddanddanserver.domain.model.pet

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.math.pow

@Document("pets")
class Pet(
    val id: ObjectId,
    val type: String,
    val ownerUserId: ObjectId,
    var exp: Int,
) {
    /** 경험치가 [previousExp]였을 때와 지금 레벨을 비교한다. */
    fun levelUpResultSince(previousExp: Int): LevelUpResult =
        LevelUpResult(
            previousLevel = levelOf(previousExp),
            currentLevel = getLevel(),
        )

    fun isOwner(userId: ObjectId): Boolean = ownerUserId == userId

    fun isMaxLevel(): Boolean = false

    fun getLevel(): Int = levelOf(exp)

    fun getExpPercent(): Double {
        val currentLevel = getLevel()
        val levelStartExp = getLevelStartExp(currentLevel)
        val levelRequiredExp = getLevelRequiredExp(currentLevel)
        val currentLevelExp = exp - levelStartExp
        return currentLevelExp.toDouble() / levelRequiredExp * 100
    }

    private fun getLevelStartExp(level: Int): Int {
        var totalExp = 0
        var requiredExp = BASE_EXP

        for (i in 1 until level) {
            totalExp += requiredExp
            if (i >= 2) requiredExp *= 2
        }

        return totalExp
    }

    private fun getLevelRequiredExp(level: Int): Int {
        return if (level <= 2) BASE_EXP else (BASE_EXP * 2.0.pow(level - 2)).toInt()
    }

    companion object {
        private const val BASE_EXP = 500
        const val EXP_PER_FOOD = 100
        const val EXP_PER_TOY = 500

        private fun levelOf(exp: Int): Int {
            var remainingExp = exp
            var level = 1
            var requiredExp = BASE_EXP

            while (remainingExp >= requiredExp) {
                remainingExp -= requiredExp
                level++
                if (level > 2) requiredExp *= 2
            }

            return level
        }

        fun register(
            type: String,
            ownerUserId: ObjectId,
        ): Pet =
            Pet(
                id = ObjectId(),
                type = type,
                ownerUserId = ownerUserId,
                exp = 0,
            )
    }
}
