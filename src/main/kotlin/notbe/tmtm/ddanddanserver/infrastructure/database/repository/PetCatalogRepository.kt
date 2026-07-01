package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PetCatalogRepository : MongoRepository<PetCatalogEntity, ObjectId> {
    fun findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc(): List<PetCatalogEntity>

    fun findAllByOrderByDisplayOrderAscTypeAsc(): List<PetCatalogEntity>

    fun findByType(type: String): PetCatalogEntity?

    fun existsByType(type: String): Boolean
}
