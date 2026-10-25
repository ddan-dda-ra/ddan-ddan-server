package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId

interface PetRepositoryCustom {
    /** 경험치를 원자적으로 올리고 갱신 후 펫을 반환한다. */
    fun increaseExp(petId: ObjectId, amount: Int): Pet
}
