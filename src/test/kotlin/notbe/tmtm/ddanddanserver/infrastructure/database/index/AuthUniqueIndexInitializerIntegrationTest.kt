package notbe.tmtm.ddanddanserver.infrastructure.database.index

import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * auths unique 인덱스 생성 통합 테스트.
 *
 * 인덱스 키 이름(snake_case)과 중복 데이터가 있을 때의 동작은 실제 MongoDB로만 확인할 수 있다.
 * 실행 조건과 DB는 UserStatRepositoryDateRangeIntegrationTest와 같다(`RUN_MONGO_IT=true`, 전용 DB ddan-ddan-it-test).
 */
@DataMongoTest
@TestPropertySource(
    properties = [
        "spring.data.mongodb.host=\${MONGO_IT_HOST:localhost}",
        "spring.data.mongodb.port=\${MONGO_IT_PORT:27017}",
        "spring.data.mongodb.database=ddan-ddan-it-test",
        "spring.data.mongodb.username=\${MONGO_IT_USER:}",
        "spring.data.mongodb.password=\${MONGO_IT_PASSWORD:}",
        "spring.data.mongodb.authentication-database=admin",
        "spring.data.mongodb.field-naming-strategy=org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy",
        "spring.data.mongodb.auto-index-creation=true",
        "de.flapdoodle.mongodb.embedded.version=",
    ],
)
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_IT", matches = "true")
class AuthUniqueIndexInitializerIntegrationTest {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var authRepository: AuthRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        mongoTemplate.remove(Query(), Auth::class.java)
        runCatching { mongoTemplate.indexOps(Auth::class.java).dropIndex(AuthUniqueIndexInitializer.INDEX_NAME) }
    }

    @Test
    fun `unique 인덱스를 만들고 같은 계정의 두 번째 Auth 저장을 막아야 한다`() {
        authRepository.save(Auth.create("oauth-1", OAuthType.KAKAO, ObjectId()))

        AuthUniqueIndexInitializer(mongoTemplate).ensureIndex()

        val index = uniqueIndex()
        assertTrue(index!!.isUnique)
        assertEquals(listOf("type", "o_auth_id"), index.indexFields.map { it.key })
        assertThrows<DuplicateKeyException> {
            authRepository.save(Auth.create("oauth-1", OAuthType.KAKAO, ObjectId()))
        }
        // 다른 제공자의 같은 id는 허용한다.
        authRepository.save(Auth.create("oauth-1", OAuthType.APPLE, ObjectId()))
    }

    @Test
    fun `기존 데이터에 중복이 있으면 인덱스를 만들지 않고 예외도 던지지 않아야 한다`() {
        authRepository.save(Auth.create("oauth-dup", OAuthType.KAKAO, ObjectId()))
        authRepository.save(Auth.create("oauth-dup", OAuthType.KAKAO, ObjectId()))

        assertDoesNotThrow { AuthUniqueIndexInitializer(mongoTemplate).ensureIndex() }

        assertNull(uniqueIndex())
    }

    @Test
    fun `이미 인덱스가 있으면 다시 실행해도 문제없어야 한다`() {
        AuthUniqueIndexInitializer(mongoTemplate).ensureIndex()
        assertDoesNotThrow { AuthUniqueIndexInitializer(mongoTemplate).ensureIndex() }
        assertTrue(uniqueIndex()!!.isUnique)
    }

    private fun uniqueIndex() =
        mongoTemplate.indexOps(Auth::class.java).indexInfo.find { it.name == AuthUniqueIndexInitializer.INDEX_NAME }
}
