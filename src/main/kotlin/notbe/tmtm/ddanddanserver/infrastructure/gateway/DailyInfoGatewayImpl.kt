package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.DailyInfoEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.respository.DailyInfoRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyInfoGatewayImpl(
    private val dailyInfoRepository: DailyInfoRepository,
) : DailyInfoGateway {
    override fun save(dailyInfo: DailyInfo): DailyInfo = dailyInfoRepository.save(DailyInfoEntity.fromDomain(dailyInfo)).toDomain()

    override fun getOrCreate(
        userId: String,
        date: LocalDate,
    ): DailyInfo =
        dailyInfoRepository
            .findByUserIdAndDate(userId, date)
            ?.toDomain()
            ?: DailyInfo.register(userId, date, calorie = 0)
}
