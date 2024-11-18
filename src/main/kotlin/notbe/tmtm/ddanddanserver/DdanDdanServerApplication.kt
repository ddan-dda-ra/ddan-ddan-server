package notbe.tmtm.ddanddanserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class DdanDdanServerApplication

fun main(args: Array<String>) {
    runApplication<DdanDdanServerApplication>(*args)
}
