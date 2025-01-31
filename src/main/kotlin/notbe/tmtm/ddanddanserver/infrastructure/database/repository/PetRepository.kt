package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface PetRepository : MongoRepository<PetEntity, String> {
    fun findAllByOwnerUserId(ownerUserId: String): List<PetEntity>

    fun deleteByOwnerUserId(ownerUserId: String)
}
