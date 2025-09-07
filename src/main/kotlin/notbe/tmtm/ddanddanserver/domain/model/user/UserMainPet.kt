package notbe.tmtm.ddanddanserver.domain.model.user

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

data class UserMainPet(
    val user: User,
    val mainPet: Pet?,
)
