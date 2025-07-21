package notbe.tmtm.ddanddanserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@Configuration
@EnableRedisRepositories
class RedisConfig {
    @Value("\${spring.data.redis.host}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port}")
    private var redisPort: Int = 6379

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory(redisHost, redisPort)

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<*, *> {
        val template = RedisTemplate<Any, Any>()
        template.connectionFactory = connectionFactory
        return template
    }
}
