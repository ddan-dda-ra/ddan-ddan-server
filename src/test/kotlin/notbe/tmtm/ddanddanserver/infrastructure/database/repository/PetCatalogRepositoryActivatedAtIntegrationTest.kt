package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 새 펫 알림의 공개 시각 조회 통합 테스트.
 *
 * derived query가 snake_case `activated_at` 필드와 구간 경계((from, to])를 올바르게 다루는지는
 * 실제 MongoDB에서만 확인할 수 있다. 실행 방법과 안전장치는 UserStatRepositoryDateRangeIntegrationTest와 같다.
 *
 * 실행: `RUN_MONGO_IT=true MONGO_IT_USER=... MONGO_IT_PASSWORD=... ./gradlew test --tests "*PetCatalogRepositoryActivatedAtIntegrationTest"`
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
        "de.flapdoodle.mongodb.embedded.version=", // embedded mongo 자동구성 비활성(의존 없음)
    ],
)
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_IT", matches = "true")
class PetCatalogRepositoryActivatedAtIntegrationTest {
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var repository: PetCatalogRepository

    private val from = Instant.parse("2026-10-27T05:00:00Z")
    private val to = Instant.parse("2026-10-28T05:00:00Z")

    @BeforeEach
    fun clean() {
        mongoTemplate.dropCollection("pet_catalog")
    }

    @AfterEach
    fun teardown() {
        mongoTemplate.dropCollection("pet_catalog")
    }

    private fun savePet(
        type: String,
        isActive: Boolean,
        activatedAt: Instant?,
    ) {
        mongoTemplate.save(
            PetCatalogEntity(
                type = type,
                name = type,
                colorCode = "#112233",
                isActive = isActive,
                levels = emptyMap(),
                activatedAt = activatedAt,
            ),
        )
    }

    private fun exists() = repository.existsActivatedBetween(from, to)

    @Test
    fun `activatedAt은 snake_case activated_at 필드로 저장된다`() {
        savePet("CAT", isActive = true, activatedAt = to)

        val doc = mongoTemplate.getCollection("pet_catalog").find().first()!!
        assertEquals(java.util.Date.from(to), doc["activated_at"])
    }

    @Test
    fun `구간 끝 시각에 공개된 펫은 포함된다`() {
        savePet("CAT", isActive = true, activatedAt = to)

        assertTrue(exists())
    }

    @Test
    fun `구간 시작 시각에 공개된 펫은 전날 알림에 포함되었으므로 제외된다`() {
        savePet("CAT", isActive = true, activatedAt = from)

        assertFalse(exists())
    }

    @Test
    fun `구간 밖에 공개된 펫, 공개 시각이 없는 펫, 비활성 펫은 제외된다`() {
        savePet("OLD", isActive = true, activatedAt = from.minusSeconds(1))
        savePet("FUTURE", isActive = true, activatedAt = to.plusSeconds(1))
        savePet("LEGACY", isActive = true, activatedAt = null)
        savePet("DRAFT", isActive = false, activatedAt = to.minusSeconds(1))

        assertFalse(exists())
    }
}
