package notbe.tmtm.ddanddanserver.domain.model.pet

data class LevelUpResult(
    val previousLevel: Int,
    val currentLevel: Int,
) {
    fun shouldReceiveTicket(): Boolean = previousLevel < 5 && currentLevel >= 5
}
