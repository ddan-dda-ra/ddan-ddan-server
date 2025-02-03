package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import java.time.LocalDate

interface DailyInfoGateway {
    fun save(dailyInfo: DailyInfo): DailyInfo

    fun findBy(
        userId: String,
        date: LocalDate,
    ): DailyInfo?

    fun getByDateBeforeNDays(
        userId: String,
        date: LocalDate,
        n: Int,
    ): List<DailyInfo>

    fun deleteByUserId(userId: String)
}
