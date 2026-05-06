package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import notbe.tmtm.ddanddanserver.application.service.StatsAdminService
import notbe.tmtm.ddanddanserver.presentation.dto.admin.StatsAdminResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/stats")
@Validated
@Tag(name = "Admin · 통계", description = "운영자용 통계 대시보드")
class StatsAdminController(
    private val statsAdminService: StatsAdminService,
) {
    @Operation(
        summary = "통계 대시보드",
        description =
            "전체/신규/활성 유저, 펫 분포, 일별 가입 추이를 한 번에 반환. " +
                "날짜는 KST(Asia/Seoul) 기준. 가입 시각은 ObjectId timestamp 사용.",
    )
    @GetMapping("/dashboard")
    fun dashboard(
        @Parameter(description = "가입 추이 시리즈 길이(일). default 14, max 90")
        @RequestParam(defaultValue = "14")
        @Min(1)
        @Max(90)
        seriesDays: Int,
    ): StatsAdminResponse = statsAdminService.getDashboard(seriesDays)
}
