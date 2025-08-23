package notbe.tmtm.ddanddanserver.domain.model.pet

enum class PetType {
    CAT,
    HAMSTER,
    PENGUIN,
    DOG,
    MOLE,
    ;

    companion object {
        fun getRandom(): PetType = entries.toTypedArray().random()

        fun getRandomWithout(types: List<PetType>): PetType {
            val availableTypes = entries.filter { it !in types }
            if (availableTypes.isEmpty()) {
                return getRandom()
            }
            return availableTypes.toTypedArray().random()
        }
    }
}
