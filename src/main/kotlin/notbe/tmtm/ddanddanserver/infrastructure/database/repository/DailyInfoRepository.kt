package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.DailyInfoEntity
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate

interface DailyInfoRepository : MongoRepository<DailyInfoEntity, String> {
    fun findByUserIdAndDate(
        userId: String,
        date: LocalDate,
    ): DailyInfoEntity?

    fun findByUserIdAndDateBetween(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailyInfoEntity>

    fun deleteByUserId(userId: String)
}
