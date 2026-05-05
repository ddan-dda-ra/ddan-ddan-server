package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.UserAdminService
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminDetailResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.UserAdminListResponse
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/users")
@Tag(name = "Admin · 유저", description = "운영자용 유저 조회")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    @Operation(summary = "유저 목록·검색")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) keyword: String?,
    ): UserAdminListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastLoginAt"))
        return UserAdminListResponse.from(userAdminService.searchUsers(keyword, pageable))
    }

    @Operation(summary = "유저 상세")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
    ): UserAdminDetailResponse = UserAdminDetailResponse.from(userAdminService.getUser(ObjectId(id)))
}
