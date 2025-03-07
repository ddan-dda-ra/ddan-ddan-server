package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.DailyInfoGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser.UpdateUserInput
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser.UpdateUserOutput
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class UpdateUser(
    private val userGateway: UserGateway,
    private val dailyInfoGateway: DailyInfoGateway,
) : UseCase<UpdateUserInput, UpdateUserOutput> {
    data class UpdateUserInput(
        val userId: String,
        val name: String,
        val purposeCalorie: Int,
    ) {
        init {
            require(name.isNotBlank()) { "name should not be blank" }
            require(purposeCalorie in 100..1000) { "purposeCalorie should be between 100 and 1000" }
        }
    }

    data class UpdateUserOutput(
        val user: User,
    )

    @Transactional
    override fun execute(input: UpdateUserInput): UpdateUserOutput {
        val user = userGateway.getById(input.userId)

        user.update(
            name = input.name,
            purposeCalorie = input.purposeCalorie,
        )

        // 데일리 데이터에 이름 갱신
        dailyInfoGateway.findBy(user.id, LocalDate.now())?.let {
            it.userName = user.name
            dailyInfoGateway.save(it)
        }

        return UpdateUserOutput(
            userGateway.update(user),
        )
    }
}
