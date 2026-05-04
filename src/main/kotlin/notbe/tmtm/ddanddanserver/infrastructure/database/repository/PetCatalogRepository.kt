package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PetCatalogRepository : MongoRepository<PetCatalogEntity, ObjectId> {
    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<PetCatalogEntity>
}
