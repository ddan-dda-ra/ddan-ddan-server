package notbe.tmtm.ddanddanserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class DdanDdanServerApplication

fun main(args: Array<String>) {
    runApplication<DdanDdanServerApplication>(*args)
}
