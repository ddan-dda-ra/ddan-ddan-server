package notbe.tmtm.ddanddanserver.infrastructure.database.respository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PetRepository : JpaRepository<PetEntity, String> {
    fun findAllByOwnerUserId(ownerUserId: String): List<PetEntity>
}
