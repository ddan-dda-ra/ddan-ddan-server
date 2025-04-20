package notbe.tmtm.ddanddanserver.migration

import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class PurposeStrictMigration {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var dailyInfoRepository: DailyInfoRepository

    @Test
    fun migrate() {
        val users = userRepository.findAll()
        users.forEach { user ->

            val date = LocalDate.now().minusDays(1)
            var strict = 0L

            while (true) {
                val dailyInfo = dailyInfoRepository.findByUserIdAndDate(user.id, date.minusDays(strict))
                if (dailyInfo == null || dailyInfo.purposeAchieved.not()) {
                    user.purposeStrict = strict.toInt()
                    dailyInfoRepository.findByUserIdAndDate(user.id, LocalDate.now())?.let {
                        if (it.purposeAchieved) {
                            strict += 1
                        }
                    }
                    break
                } else {
                    strict += 1
                }
            }

            println("userId: ${user.name}, strict: $strict")
            userRepository.save(user)
        }
    }
}
