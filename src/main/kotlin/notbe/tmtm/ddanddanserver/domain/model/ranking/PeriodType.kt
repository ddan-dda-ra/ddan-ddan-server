package notbe.tmtm.ddanddanserver.domain.model.ranking

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    fun currentStartDate(): LocalDate =
        when (this) {
            DAILY -> LocalDate.now()
            WEEKLY -> LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            MONTHLY -> LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
            YEARLY -> LocalDate.now().with(TemporalAdjusters.firstDayOfYear())
        }

    fun currentEndDate(): LocalDate =
        when (this) {
            DAILY -> LocalDate.now()
            WEEKLY -> LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            MONTHLY -> LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
            YEARLY -> LocalDate.now().with(TemporalAdjusters.lastDayOfYear())
        }
}
