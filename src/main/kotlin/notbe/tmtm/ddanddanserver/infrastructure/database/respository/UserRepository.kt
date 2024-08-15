package notbe.tmtm.ddanddanserver.infrastructure.database.respository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, String>
