package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.RankingService
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.presentation.dto.response.RankingResponse
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/ranking")
@Tag(name = "랭킹")
class RankingController(
    private val rankingService: RankingService,
) {
    @GetMapping
    @Operation(summary = "랭킹 조회", description = "랭킹을 조회합니다.")
    fun getRanking(
        authentication: Authentication,
        @RequestParam criteria: RankingCriteria,
        @RequestParam periodType: PeriodType,
    ): RankingResponse {
        val result =
            rankingService.getRanking(
                userId = ObjectId(authentication.name),
                criteria = criteria,
                periodType = periodType,
            )

        return RankingResponse.fromDomain(
            criteria = criteria,
            periodType = periodType,
            result = result,
        )
    }
}
