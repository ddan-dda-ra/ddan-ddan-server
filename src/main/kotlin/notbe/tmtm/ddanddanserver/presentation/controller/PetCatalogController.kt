package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetCatalogResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pets")
@Tag(name = "펫 카탈로그", description = "사용 가능한 펫 메타데이터와 레벨별 미디어 URL 제공")
class PetCatalogController(
    private val petCatalogService: PetCatalogService,
) {
    @Operation(summary = "펫 카탈로그 조회", description = "활성 펫 목록과 revision을 항상 200으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @GetMapping("/catalog")
    fun getCatalog(): PetCatalogResponse = PetCatalogResponse.from(petCatalogService.getCatalog())
}
