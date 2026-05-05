package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminCreateRequest
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminItemResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminListResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminUpdateRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/admin/pet-catalog")
@Tag(name = "Admin · 펫 카탈로그", description = "운영자용 펫 카탈로그 CRUD")
@ConditionalOnProperty(prefix = "admin", name = ["enabled"], havingValue = "true")
class PetCatalogAdminController(
    private val petCatalogService: PetCatalogService,
) {
    @Operation(summary = "전체 펫 카탈로그 조회 (비활성 포함)")
    @GetMapping
    fun list(): PetCatalogAdminListResponse = PetCatalogAdminListResponse.from(petCatalogService.getAllForAdmin())

    @Operation(summary = "펫 카탈로그 신규 등록")
    @PostMapping
    fun create(
        @RequestBody @Valid request: PetCatalogAdminCreateRequest,
    ): PetCatalogAdminItemResponse =
        PetCatalogAdminItemResponse.from(
            petCatalogService.create(
                key = request.key,
                name = request.name,
                isActive = request.isActive,
                displayOrder = request.displayOrder,
                levels = request.levelsToDomain(),
            ),
        )

    @Operation(summary = "펫 카탈로그 수정")
    @PutMapping("/{key}")
    fun update(
        @PathVariable key: String,
        @RequestBody @Valid request: PetCatalogAdminUpdateRequest,
    ): PetCatalogAdminItemResponse =
        PetCatalogAdminItemResponse.from(
            petCatalogService.update(
                key = key,
                name = request.name,
                isActive = request.isActive,
                displayOrder = request.displayOrder,
                levels = request.levelsToDomain(),
            ),
        )

    @Operation(summary = "펫 카탈로그 비활성화 (soft delete)")
    @DeleteMapping("/{key}")
    fun softDelete(
        @PathVariable key: String,
    ): PetCatalogAdminItemResponse = PetCatalogAdminItemResponse.from(petCatalogService.softDelete(key))
}
