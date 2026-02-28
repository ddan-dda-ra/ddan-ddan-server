package notbe.tmtm.ddanddanserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryConfig {

    @Bean
    fun retryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(500)
            .build()
    }
}
