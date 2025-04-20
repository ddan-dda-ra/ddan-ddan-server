package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DailyInfoService(
    private val dailyInfoRepository: DailyInfoRepository,
) {
    fun getAllPurposeSuccess(date: LocalDate): List<DailyInfo> = dailyInfoRepository.findAllByDateAndPurposeAchieved(date, true)
}
