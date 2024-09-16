package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import java.time.LocalDate

interface DailyInfoGateway {
    fun save(dailyInfo: DailyInfo): DailyInfo

    fun getOrCreate(
        userId: String,
        date: LocalDate,
    ): DailyInfo
}
