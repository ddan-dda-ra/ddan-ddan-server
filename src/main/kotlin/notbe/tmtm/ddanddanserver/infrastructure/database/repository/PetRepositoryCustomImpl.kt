package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAndModify
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class PetRepositoryCustomImpl(
    private val mongoTemplate: MongoTemplate,
) : PetRepositoryCustom {
    override fun increaseExp(petId: ObjectId, amount: Int): Pet =
        mongoTemplate.findAndModify<Pet>(
            Query(Pet::id isEqualTo petId),
            Update().inc(Pet::exp.name, amount),
            FindAndModifyOptions.options().returnNew(true),
        ) ?: throw PetNotFoundException("Pet not found with id: $petId")
}
