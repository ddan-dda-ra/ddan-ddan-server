package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DailyInfoService(
    private val dailyInfoRepository: DailyInfoRepository,
) {
    fun getAllPurposeSuccess(date: LocalDate): List<DailyInfo> = dailyInfoRepository.findAllByDateAndPurposeAchieved(date, true)

    fun getByUserIdAndDate(
        userId: ObjectId,
        date: LocalDate,
    ): DailyInfo? = dailyInfoRepository.findByUserIdAndDate(userId, date)

    fun updateUserName(
        userId: ObjectId,
        date: LocalDate,
        userName: String?,
    ) = dailyInfoRepository.updateUserName(userId, date, userName)
}
