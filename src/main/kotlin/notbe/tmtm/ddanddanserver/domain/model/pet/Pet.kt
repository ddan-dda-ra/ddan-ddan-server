package notbe.tmtm.ddanddanserver.domain.model.pet

import notbe.tmtm.ddanddanserver.common.util.generateTsid

class Pet(
    val id: String,
    val type: PetType,
    val ownerUserId: String,
    var exp: Int,
) {
    fun getLevel(): Level = Level.entries.first { exp in it.expRange }

    fun getExpPercent(): Double {
        val currentLevel = getLevel()
        val currentLevelExp = exp - currentLevel.expRange.first
        return currentLevelExp.toDouble() / currentLevel.requestExp * 100
    }

    enum class Level(
        val level: Int,
        val expRange: IntRange,
        val requestExp: Int,
    ) {
        ONE(1, 0..299, 300),
        TWO(2, 300..899, 600),
        THREE(3, 900..1599, 700),
        FOUR(4, 1600..3499, 1900),
        FIVE(5, 3500..7000, 3500),
    }

    companion object {
        fun register(
            type: PetType,
            ownerUserId: String,
        ): Pet =
            Pet(
                id = generateTsid(),
                type = type,
                ownerUserId = ownerUserId,
                exp = 0,
            )
    }
}
