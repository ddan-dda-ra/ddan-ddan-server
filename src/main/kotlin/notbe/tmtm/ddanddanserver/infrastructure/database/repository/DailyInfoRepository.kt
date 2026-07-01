package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate

interface DailyInfoRepository : MongoRepository<DailyInfo, ObjectId> {
    fun findByUserIdAndDate(
        userId: ObjectId,
        date: LocalDate,
    ): DailyInfo?

    fun findAllByDateAndPurposeAchieved(
        date: LocalDate,
        purposeAchieved: Boolean,
    ): List<DailyInfo>

    fun deleteAllByUserId(userId: ObjectId)
}
