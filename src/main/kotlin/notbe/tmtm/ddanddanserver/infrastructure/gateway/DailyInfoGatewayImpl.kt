package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyInfoGatewayImpl(
    private val dailyInfoRepository: DailyInfoRepository,
) : DailyInfoGateway {
    override fun save(dailyInfo: DailyInfo): DailyInfo = dailyInfoRepository.save(dailyInfo)

    override fun findBy(
        userId: ObjectId,
        date: LocalDate,
    ): DailyInfo? = dailyInfoRepository.findByUserIdAndDate(userId, date)

    override fun getByDateBeforeNDays(
        userId: ObjectId,
        date: LocalDate,
        n: Int,
    ): List<DailyInfo> {
        val startDate = date.minusDays(n.toLong())
        return dailyInfoRepository.findByUserIdAndDateBetween(userId, startDate, date)
    }

    override fun deleteByUserId(userId: ObjectId) = dailyInfoRepository.deleteByUserId(userId)
}
