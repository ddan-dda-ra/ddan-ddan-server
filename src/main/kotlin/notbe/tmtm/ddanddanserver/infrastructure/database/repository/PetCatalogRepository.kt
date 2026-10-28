package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface PetCatalogRepository : MongoRepository<PetCatalogEntity, ObjectId> {
    fun findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc(): List<PetCatalogEntity>

    fun findTopByIsActiveTrueOrderByUpdatedAtDesc(): PetCatalogEntity?

    fun findByType(type: String): PetCatalogEntity?

    fun existsByType(type: String): Boolean

    /** (from, to] 사이에 공개된 펫이 있는지 확인한다. derived query는 같은 필드에 조건을 두 번 걸 수 없어 직접 작성한다. */
    @Query(value = "{ 'is_active': true, 'activated_at': { '\$gt': ?0, '\$lte': ?1 } }", exists = true)
    fun existsActivatedBetween(
        from: Instant,
        to: Instant,
    ): Boolean
}
