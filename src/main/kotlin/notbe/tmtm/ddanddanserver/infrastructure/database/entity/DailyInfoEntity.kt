package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import java.time.LocalDate

@Entity(name = "calorie_daily_infos")
data class DailyInfoEntity(
    @Id
    @Column(name = "id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val id: String,
    @Column(name = "user_id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val userId: String,
    @Column(name = "date", nullable = false)
    val date: LocalDate,
    @Column(name = "calorie", nullable = false)
    val calorie: Int,
) : BaseEntity() {
    fun toDomain() =
        DailyInfo(
            id = id,
            userId = userId,
            date = date,
            calorie = calorie,
        )

    companion object {
        fun fromDomain(dailyInfo: DailyInfo) =
            with(dailyInfo) {
                DailyInfoEntity(
                    id = id,
                    userId = userId,
                    date = date,
                    calorie = calorie,
                )
            }
    }
}
