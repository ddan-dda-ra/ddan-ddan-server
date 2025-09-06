package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.CheerService
import notbe.tmtm.ddanddanserver.presentation.dto.response.CheerResponse
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/cheers")
@Tag(name = "응원하기")
class CheerController(
    private val cheerService: CheerService,
) {
    @PostMapping("/{friendId}")
    @Operation(
        summary = "친구 응원하기",
        description = "친구에게 응원을 보냅니다. 하루에 한 번만 가능하며, 친구 관계인 사용자에게만 응원할 수 있습니다."
    )
    fun cheerFriend(
        authentication: Authentication,
        @Parameter(description = "응원할 친구의 사용자 ID", required = true, example = "64a1b2c3d4e5f6789012346")
        @PathVariable friendId: String,
    ): CheerResponse {
        val userId = ObjectId(authentication.name)
        val friendObjectId = ObjectId(friendId)

        val cheer = cheerService.createCheer(userId, friendObjectId)
        return CheerResponse.fromDomain(cheer)
    }
}
