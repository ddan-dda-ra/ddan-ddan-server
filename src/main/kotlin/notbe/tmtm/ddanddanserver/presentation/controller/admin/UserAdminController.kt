package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import notbe.tmtm.ddanddanserver.application.service.UserAdminService
import notbe.tmtm.ddanddanserver.application.service.UserAdminSortType
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminDetailResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminListResponse
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/users")
@Validated
@Tag(name = "Admin · 유저", description = "운영자용 유저 조회")
@ConditionalOnProperty(prefix = "admin", name = ["enabled"], havingValue = "true")
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
    ): UserAdminListResponse =
        UserAdminListResponse.from(
            userAdminService.searchUsers(
                keyword = keyword,
                sortType = sort,
                page = page,
                size = size,
            ),
        )

    @Operation(summary = "유저 상세")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
    ): UserAdminDetailResponse = UserAdminDetailResponse.from(userAdminService.getUser(ObjectId(id)))
}
