package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class MonthlyRankingService(
    private val userStatRepository: UserStatRepository,
    private val discordHookApi: DiscordHookApi,
    private val petCatalogService: PetCatalogService,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String,
) {
    fun sendPreviousMonthRanking() {
        val today = LocalDate.now(ZoneId.of(KST))
        val prevStart = today.minusMonths(1).withDayOfMonth(1)
        val prevEnd = prevStart.withDayOfMonth(prevStart.lengthOfMonth())
        val prevPrevStart = today.minusMonths(2).withDayOfMonth(1)
        val prevPrevEnd = prevPrevStart.withDayOfMonth(prevPrevStart.lengthOfMonth())

        val embeds = buildList {
            runCatching {
                add(buildCaloriesEmbed(top(RankingCriteria.TOTAL_CALORIES, prevStart, prevEnd), prevStart))
            }.onFailure { logger().error("칼로리 embed 생성 실패: month={}", prevStart.month, it) }
            runCatching {
                add(buildSucceededDaysEmbed(top(RankingCriteria.TOTAL_SUCCEEDED_DAYS, prevStart, prevEnd), prevStart))
            }.onFailure { logger().error("목표달성 embed 생성 실패: month={}", prevStart.month, it) }
            runCatching {
                add(buildAttendanceEmbed(top(RankingCriteria.TOTAL_ATTENDANCE_DAYS, prevStart, prevEnd), prevStart))
            }.onFailure { logger().error("출석 embed 생성 실패: month={}", prevStart.month, it) }
            runCatching {
                val risers = computeRankRisers(
                    RankingCriteria.TOTAL_CALORIES,
                    prevPrevStart,
                    prevPrevEnd,
                    prevStart,
                    prevEnd,
                )
                add(
                    buildRankRiseEmbed(
                        risers,
                        prevStart,
                        "📈 ${prevStart.monthValue}월 칼로리 순위 상승왕 TOP $TOP_N",
                        COLOR_RANK_RISE,
                        withFooter = false,
                    ),
                )
            }.onFailure { logger().error("칼로리 순위 상승 embed 생성 실패: month={}", prevStart.month, it) }
            runCatching {
                val risers = computeRankRisers(
                    RankingCriteria.TOTAL_SUCCEEDED_DAYS,
                    prevPrevStart,
                    prevPrevEnd,
                    prevStart,
                    prevEnd,
                )
                add(
                    buildRankRiseEmbed(
                        risers,
                        prevStart,
                        "📈 ${prevStart.monthValue}월 목표달성 순위 상승왕 TOP $TOP_N",
                        COLOR_RANK_RISE_SUCCEEDED,
                        withFooter = true,
                    ),
                )
            }.onFailure { logger().error("목표달성 순위 상승 embed 생성 실패: month={}", prevStart.month, it) }
        }

        if (embeds.isEmpty()) {
            logger().error("월간 랭킹 발송 스킵: 생성된 embed 없음 month={}", prevStart.month)
            return
        }
        runCatching { discordHookApi.sendMessage(DiscordHookApi.Request(embeds = embeds)) }
            .onFailure { logger().error("월간 랭킹 Discord 발송 실패: month={}", prevStart.month, it) }
    }

    private fun top(
        criteria: RankingCriteria,
        from: LocalDate,
        to: LocalDate,
    ): List<UserStatEntity> = userStatRepository.findRankingByDateRange(criteria, from, to, TOP_N)

    private fun assignRanks(ranking: List<UserStatEntity>): Map<ObjectId, Int> =
        ranking.mapIndexed { index, stat -> stat.user.id to (index + 1) }.toMap()

    private fun computeRankRisers(
        criteria: RankingCriteria,
        prevPrevStart: LocalDate,
        prevPrevEnd: LocalDate,
        prevStart: LocalDate,
        prevEnd: LocalDate,
    ): List<RankRise> {
        val prevPrevRanks = assignRanks(
            userStatRepository.findAllRankingByDateRange(criteria, prevPrevStart, prevPrevEnd),
        )
        val prevRanking = userStatRepository.findAllRankingByDateRange(criteria, prevStart, prevEnd)
        val prevRanks = assignRanks(prevRanking)

        return prevRanking
            .mapNotNull { stat ->
                val userId = stat.user.id
                val before = prevPrevRanks[userId] ?: return@mapNotNull null
                val after = prevRanks.getValue(userId)
                val delta = before - after
                RankRise(stat = stat, delta = delta, prevRank = before, curRank = after)
            }
            .filter { it.delta > 0 }
            .sortedWith(
                compareByDescending<RankRise> { it.delta }
                    .thenBy { it.curRank },
            )
            .take(TOP_N)
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
        )

    private fun buildAttendanceEmbed(
        top: List<UserStatEntity>,
        month: LocalDate,
    ): DiscordHookApi.Embed =
        DiscordHookApi.Embed(
            title = "📅 ${month.monthValue}월 출석왕 TOP $TOP_N",
            color = COLOR_ATTENDANCE,
            description = top.toAttendanceDescription(),
        )

    private fun buildRankRiseEmbed(
        risers: List<RankRise>,
        month: LocalDate,
        title: String,
        color: Int,
        withFooter: Boolean,
    ): DiscordHookApi.Embed =
        DiscordHookApi.Embed(
            title = title,
            color = color,
            description = risers.toRankRiseDescription(),
            footer = if (withFooter) DiscordHookApi.Footer(text = footerText(month)) else null,
            timestamp = if (withFooter) Instant.now().toString() else null,
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

    private fun List<UserStatEntity>.toAttendanceDescription(): String {
        if (isEmpty()) return "_지난 달 데이터가 없습니다._"
        return mapIndexed { index, stat ->
            "${medal(index)} **${stat.user.name ?: "(이름 없음)"}** — " +
                "${stat.totalAttendanceDays}일 출석 · " +
                "${"%,d".format(stat.totalCalories)} cal · " +
                petLabel(stat)
        }.joinToString("\n")
    }

    private fun List<RankRise>.toRankRiseDescription(): String {
        if (isEmpty()) return "_지난 달 순위 상승자가 없습니다._"
        return mapIndexed { index, rise ->
            "${medal(index)} **${rise.stat.user.name ?: "(이름 없음)"}** — " +
                "▲${rise.delta}계단 (${rise.prevRank}위 → ${rise.curRank}위) · " +
                petLabel(rise.stat)
        }.joinToString("\n")
    }

    private fun petLabel(stat: UserStatEntity): String {
        val key = stat.mainPet.type
        val label = petCatalogService.getName(key) ?: key
        return "$label Lv.${stat.mainPet.getLevel()}"
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

    private data class RankRise(
        val stat: UserStatEntity,
        val delta: Int,
        val prevRank: Int,
        val curRank: Int,
    )

    companion object {
        private const val TOP_N = 3
        private const val KST = "Asia/Seoul"
        private const val COLOR_CALORIES = 0xFF7F00
        private const val COLOR_SUCCEEDED_DAYS = 0x57F287
        private const val COLOR_ATTENDANCE = 0x5865F2
        private const val COLOR_RANK_RISE = 0xFEE75C
        private const val COLOR_RANK_RISE_SUCCEEDED = 0xEB459E
    }
}
