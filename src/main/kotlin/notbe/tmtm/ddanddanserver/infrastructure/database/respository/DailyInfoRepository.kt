package notbe.tmtm.ddanddanserver.infrastructure.database.respository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.DailyInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyInfoRepository : JpaRepository<DailyInfoEntity, String> {
    fun findByUserIdAndDate(
        userId: String,
        date: LocalDate,
    ): DailyInfoEntity?
}
