package notbe.tmtm.ddanddanserver.domain.model.pet

enum class PetType {
    CAT,
    HAMSTER,
    PENGUIN,
    DOG,
    ;

    companion object {
        fun getRandom(): PetType = entries.toTypedArray().random()
    }
}
