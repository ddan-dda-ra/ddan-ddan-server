package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import notbe.tmtm.ddanddanserver.application.service.UserAdminService
import notbe.tmtm.ddanddanserver.application.service.UserAdminSortType
import notbe.tmtm.ddanddanserver.presentation.dto.admin.DailyCaloriesAdminListResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminDetailResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminListResponse
import org.bson.types.ObjectId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/v1/admin/users")
@Validated
@Tag(name = "Admin · 유저", description = "운영자용 유저 조회")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    @Operation(summary = "유저 목록·검색 (페이징)")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "LATEST_LOGIN") sort: UserAdminSortType,
    ): UserAdminListResponse {
        val pageResult =
            userAdminService.searchUsers(
                keyword = keyword,
                sortType = sort,
                page = page,
                size = size,
            )
        val mainPetTypes = userAdminService.getMainPetTypes(pageResult.content)
        return UserAdminListResponse.from(pageResult, mainPetTypes)
    }

    @Operation(summary = "유저 상세")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
    ): UserAdminDetailResponse {
        val user = userAdminService.getUser(ObjectId(id))
        val mainPetType = userAdminService.getMainPetType(user)
        return UserAdminDetailResponse.from(user, mainPetType)
    }

    @Operation(
        summary = "유저 일별 칼로리 기록 조회",
        description = "기간 미지정 시 최근 30일. from/to는 YYYY-MM-DD 형식. 최대 조회 기간 366일.",
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "400", description = "기간 파라미터 오류 (from > to 또는 366일 초과)")
    @ApiResponse(responseCode = "404", description = "유저 미존재")
    @GetMapping("/{id}/daily-calories")
    fun dailyCalories(
        @Parameter(description = "유저 ObjectId hex string", required = true)
        @PathVariable
        id: String,
        @Parameter(description = "조회 시작일 YYYY-MM-DD (미지정 시 to-29일)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate?,
        @Parameter(description = "조회 종료일 YYYY-MM-DD (미지정 시 오늘)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate?,
    ): DailyCaloriesAdminListResponse {
        val effectiveTo = to ?: LocalDate.now()
        val effectiveFrom = from ?: effectiveTo.minusDays(DEFAULT_RANGE_DAYS)

        require(!effectiveFrom.isAfter(effectiveTo)) { "from은 to보다 이전이어야 합니다." }
        require(java.time.temporal.ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) <= MAX_RANGE_DAYS) {
            "조회 기간은 최대 ${MAX_RANGE_DAYS + 1}일 입니다."
        }

        val records =
            userAdminService.getDailyCalories(
                userId = ObjectId(id),
                from = effectiveFrom,
                to = effectiveTo,
            )
        return DailyCaloriesAdminListResponse.from(
            userId = id,
            from = effectiveFrom.toString(),
            to = effectiveTo.toString(),
            records = records,
        )
    }

    companion object {
        private const val DEFAULT_RANGE_DAYS: Long = 29 // (오늘 포함 30일)
        private const val MAX_RANGE_DAYS: Long = 365 // 최대 366일 조회 (윤년 포함)
    }
}
