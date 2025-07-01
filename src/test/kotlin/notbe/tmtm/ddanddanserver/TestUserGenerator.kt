package notbe.tmtm.ddanddanserver

import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
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
        val user = userRepository.findByIdOrThrow(ObjectId("67d6e30294ff6e2781f4f2d5"))

        val accessToken = jwtTokenProvider.createAccessToken(user)

        println("Test User Access Token: $accessToken")
    }
}
