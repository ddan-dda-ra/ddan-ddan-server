package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import org.bson.types.ObjectId
import java.time.LocalDate

interface DailyInfoGateway {
    fun save(dailyInfo: DailyInfo): DailyInfo

    fun findBy(
        userId: ObjectId,
        date: LocalDate,
    ): DailyInfo?
}
