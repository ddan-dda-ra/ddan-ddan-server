package notbe.tmtm.ddanddanserver.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.repository.CrudRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Configuration
class TestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}

@SpringBootTest(classes = [RedisConfig::class, TestConfig::class])
@Testcontainers
class RedisRepositoryTest {
    @Autowired
    private lateinit var userCacheRepository: UserCacheRepository

    companion object {
        @Container
        @JvmStatic
        private val redis =
            GenericContainer(DockerImageName.parse("redis:7.0-alpine"))
                .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Test
    fun `사용자 캐시 저장 및 조회가 성공해야 한다`() {
        // given
        val userCache =
            UserCache(
                id = "user123",
                name = "홍길동",
                email = "hong@example.com",
                level = 5,
                active = true,
            )

        // when
        userCacheRepository.save(userCache)
        val foundUser = userCacheRepository.findById("user123")

        // then
        assertTrue(foundUser.isPresent)
        assertEquals(userCache.id, foundUser.get().id)
        assertEquals(userCache.name, foundUser.get().name)
        assertEquals(userCache.email, foundUser.get().email)
        assertEquals(userCache.level, foundUser.get().level)
        assertEquals(userCache.active, foundUser.get().active)

        // cleanup
        userCacheRepository.deleteById("user123")
    }

    @Test
    fun `사용자 캐시 수정이 성공해야 한다`() {
        // given
        val userCache =
            UserCache(
                id = "user456",
                name = "김철수",
                email = "kim@example.com",
                level = 3,
                active = true,
            )
        userCacheRepository.save(userCache)

        // when
        val updatedUser = userCache.copy(name = "김영희", level = 4)
        userCacheRepository.save(updatedUser)
        val foundUser = userCacheRepository.findById("user456")

        // then
        assertTrue(foundUser.isPresent)
        assertEquals("김영희", foundUser.get().name)
        assertEquals(4, foundUser.get().level)

        // cleanup
        userCacheRepository.deleteById("user456")
    }

    @Test
    fun `사용자 캐시 삭제가 성공해야 한다`() {
        // given
        val userCache =
            UserCache(
                id = "user789",
                name = "이영수",
                email = "lee@example.com",
                level = 2,
                active = false,
            )
        userCacheRepository.save(userCache)

        // when
        userCacheRepository.deleteById("user789")
        val foundUser = userCacheRepository.findById("user789")

        // then
        assertTrue(foundUser.isEmpty)
    }

    @Test
    fun `존재하지 않는 사용자 조회 시 empty를 반환해야 한다`() {
        // when
        val foundUser = userCacheRepository.findById("nonexistent")

        // then
        assertTrue(foundUser.isEmpty)
    }

    @Test
    fun `모든 사용자 조회가 성공해야 한다`() {
        // given
        val users =
            listOf(
                UserCache("user1", "사용자1", "user1@example.com", 1, true),
                UserCache("user2", "사용자2", "user2@example.com", 2, false),
                UserCache("user3", "사용자3", "user3@example.com", 3, true),
            )
        userCacheRepository.saveAll(users)

        // when
        val allUsers = userCacheRepository.findAll().toList()

        // then
        assertTrue(allUsers.size >= 3)
        assertTrue(allUsers.any { it.id == "user1" })
        assertTrue(allUsers.any { it.id == "user2" })
        assertTrue(allUsers.any { it.id == "user3" })

        // cleanup
        userCacheRepository.deleteAll(users)
    }

    @Test
    fun `사용자 존재 여부 확인이 성공해야 한다`() {
        // given
        val userCache =
            UserCache(
                id = "existsTest",
                name = "존재테스트",
                email = "exists@example.com",
                level = 1,
                active = true,
            )
        userCacheRepository.save(userCache)

        // when & then
        assertTrue(userCacheRepository.existsById("existsTest"))
        assertTrue(!userCacheRepository.existsById("nonexistent"))

        // cleanup
        userCacheRepository.deleteById("existsTest")
    }
}

@RedisHash("user_cache")
data class UserCache(
    @Id val id: String,
    val name: String,
    val email: String,
    val level: Int,
    val active: Boolean,
)

interface UserCacheRepository : CrudRepository<UserCache, String>
