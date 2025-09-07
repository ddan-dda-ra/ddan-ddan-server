package notbe.tmtm.ddanddanserver

import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@Disabled
class TestUserGenerator {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jwtTokenProvider: JWTTokenProvider

    @Test
    fun generateTestUser() {
        val users = userRepository.findAll()

        val tokens = users.map { user ->
            "${user.name}: ${jwtTokenProvider.createAccessToken(user)}"
        }

        tokens.forEach { println(it) }
    }
}
