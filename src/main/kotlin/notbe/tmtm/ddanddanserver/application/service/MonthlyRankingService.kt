package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class MonthlyRankingService(
    private val userStatRepository: UserStatRepository,
    private val discordHookApi: DiscordHookApi,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String,
) {
    fun sendPreviousMonthRanking() {
        val today = LocalDate.now(ZoneId.of(KST))
        val previousMonthStart = today.minusMonths(1).withDayOfMonth(1)
        val previousMonthEnd = previousMonthStart.withDayOfMonth(previousMonthStart.lengthOfMonth())

        try {
            val caloriesTop = userStatRepository.findRankingByDateRange(
                RankingCriteria.TOTAL_CALORIES,
                previousMonthStart,
                previousMonthEnd,
                TOP_N,
            )
            val succeededDaysTop = userStatRepository.findRankingByDateRange(
                RankingCriteria.TOTAL_SUCCEEDED_DAYS,
                previousMonthStart,
                previousMonthEnd,
                TOP_N,
            )

            val embeds = listOf(
                buildCaloriesEmbed(caloriesTop, previousMonthStart),
                buildSucceededDaysEmbed(succeededDaysTop, previousMonthStart),
            )

            discordHookApi.sendMessage(DiscordHookApi.Request(embeds = embeds))
        } catch (e: Exception) {
            logger().error(
                "월간 랭킹 발송 실패: month={}",
                previousMonthStart.month,
                e,
            )
        }
    }

    private fun buildCaloriesEmbed(
        top: List<UserStatEntity>,
        month: LocalDate,
    ): DiscordHookApi.Embed =
        DiscordHookApi.Embed(
            title = "🔥 ${month.monthValue}월 칼로리 TOP $TOP_N",
            color = COLOR_CALORIES,
            description = top.toCaloriesDescription(),
        )

    private fun buildSucceededDaysEmbed(
        top: List<UserStatEntity>,
        month: LocalDate,
    ): DiscordHookApi.Embed =
        DiscordHookApi.Embed(
            title = "🎯 ${month.monthValue}월 목표달성일 TOP $TOP_N",
            color = COLOR_SUCCEEDED_DAYS,
            description = top.toSucceededDaysDescription(),
            footer = DiscordHookApi.Footer(text = footerText(month)),
            timestamp = Instant.now().toString(),
        )

    private fun List<UserStatEntity>.toCaloriesDescription(): String {
        if (isEmpty()) return "_지난 달 데이터가 없습니다._"
        return mapIndexed { index, stat ->
            "${medal(index)} **${stat.user.name ?: "(이름 없음)"}** — " +
                "${"%,d".format(stat.totalCalories)} cal · " +
                "달성 ${stat.totalSucceededDays}일 · " +
                petLabel(stat)
        }.joinToString("\n")
    }

    private fun List<UserStatEntity>.toSucceededDaysDescription(): String {
        if (isEmpty()) return "_지난 달 데이터가 없습니다._"
        return mapIndexed { index, stat ->
            "${medal(index)} **${stat.user.name ?: "(이름 없음)"}** — " +
                "${stat.totalSucceededDays}일 · " +
                "${"%,d".format(stat.totalCalories)} cal · " +
                petLabel(stat)
        }.joinToString("\n")
    }

    private fun petLabel(stat: UserStatEntity): String =
        "${stat.mainPet.type.toKorean()} Lv.${stat.mainPet.getLevel()}"

    private fun PetType.toKorean(): String =
        when (this) {
            PetType.CAT -> "고양이"
            PetType.HAMSTER -> "햄스터"
            PetType.PENGUIN -> "펭귄"
            PetType.DOG -> "강아지"
            PetType.MOLE -> "두더지"
        }

    private fun medal(zeroBasedIndex: Int): String =
        when (zeroBasedIndex) {
            0 -> "🥇"
            1 -> "🥈"
            2 -> "🥉"
            else -> "${zeroBasedIndex + 1}."
        }

    private fun footerText(month: LocalDate): String {
        val phase = activeProfile.uppercase()
        val phaseIcon = when (phase) {
            "PROD" -> "🚀"
            "DEV" -> "🧪"
            else -> "💻"
        }
        return "$phaseIcon $phase · ${month.year}년 ${month.monthValue}월 월간 랭킹 · ddan-ddan-server"
    }

    companion object {
        private const val TOP_N = 3
        private const val KST = "Asia/Seoul"
        private const val COLOR_CALORIES = 0xFF7F00
        private const val COLOR_SUCCEEDED_DAYS = 0x57F287
    }
}
