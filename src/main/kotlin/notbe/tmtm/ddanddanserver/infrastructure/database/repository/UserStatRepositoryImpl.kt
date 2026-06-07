package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDate
import kotlin.reflect.KProperty

@Repository
class UserStatRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : UserStatRepository {
    override fun findAllRankingBy(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStatEntity> {
        val matchCriteria = getMatchCriteria(periodType)
        val groupStage = getGroupStage()
        val lookupUser = getLookupUser()
        val lookupPet = getLookupPet()
        val sort = getSort(criteria)

        val agg = Aggregation.newAggregation(
            matchCriteria,
            groupStage,
            lookupUser,
            Aggregation.unwind("user", false),
            lookupPet,
            Aggregation.unwind("main_pet", false),
            sort,
        )
        return mongoTemplate.aggregate(agg, DailyInfo::class.java, UserStatEntity::class.java)
            .mappedResults
    }

    override fun findRankingByDateRange(
        criteria: RankingCriteria,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): List<UserStatEntity> {
        val stages = dateRangePipeline(criteria, startDate, endDate) + Aggregation.limit(limit.toLong())
        return aggregate(stages)
    }

    override fun findAllRankingByDateRange(
        criteria: RankingCriteria,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<UserStatEntity> {
        val start = System.nanoTime()
        val beforeHeap = usedHeapBytes()

        return aggregate(
            stages = dateRangePipeline(criteria, startDate, endDate),
            options = AggregationOptions
                .builder()
                .allowDiskUse(true)
                .maxTime(Duration.ofSeconds(FULL_RANKING_MAX_TIME_SECONDS))
                .build(),
        ).also { result ->
            val durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis()
            val heapDeltaBytes = usedHeapBytes() - beforeHeap
            logger().info(
                "전체 기간 랭킹 집계 완료: criteria={}, startDate={}, endDate={}, resultCount={}, durationMs={}, heapDeltaBytes={}",
                criteria,
                startDate,
                endDate,
                result.size,
                durationMs,
                heapDeltaBytes,
            )
            if (result.size >= FULL_RANKING_RESULT_WARN_THRESHOLD) {
                logger().warn(
                    "전체 기간 랭킹 집계 결과 수가 임계치 이상입니다: criteria={}, startDate={}, endDate={}, resultCount={}, threshold={}",
                    criteria,
                    startDate,
                    endDate,
                    result.size,
                    FULL_RANKING_RESULT_WARN_THRESHOLD,
                )
            }
        }
    }

    private fun dateRangePipeline(
        criteria: RankingCriteria,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AggregationOperation> = listOf(
        Aggregation.match(
            Criteria
                .where(DailyInfo::date.name)
                .gte(startDate)
                .lte(endDate),
        ),
        getGroupStage(),
        getLookupUser(),
        Aggregation.unwind("user", false),
        getLookupPet(),
        Aggregation.unwind("main_pet", false),
        getSort(criteria),
    )

    private fun aggregate(
        stages: List<AggregationOperation>,
        options: AggregationOptions? = null,
    ): List<UserStatEntity> {
        val aggregation = Aggregation
            .newAggregation(stages)
            .let { if (options == null) it else it.withOptions(options) }

        return mongoTemplate
            .aggregate(aggregation, DailyInfo::class.java, UserStatEntity::class.java)
            .mappedResults
    }

    private fun getMatchCriteria(periodType: PeriodType) = Aggregation.match(
        Criteria
            .where(DailyInfo::date.name)
            .gte(periodType.currentStartDate())
            .lte(periodType.currentEndDate()),
    )

    private fun getGroupStage() = Aggregation.group(DailyInfo::userId.name)
        .sum(DailyInfo::calorie.name)
        .`as`(UserStatEntity::totalCalories.toSnakeCase())
        .sum(Cond.`when`(DailyInfo::purposeAchieved.toSnakeCase()).then(1).otherwise(0))
        .`as`(UserStatEntity::totalSucceededDays.toSnakeCase())
        .count()
        .`as`(UserStatEntity::totalAttendanceDays.toSnakeCase())

    private fun getLookupUser() = Aggregation.lookup()
        .from("users")
        .localField("_id")
        .foreignField("_id")
        .pipeline(Aggregation.limit(1))
        .`as`("user")

    private fun getLookupPet() = Aggregation.lookup()
        .from("pets")
        .localField("user.main_pet_id")
        .foreignField("_id")
        .pipeline(Aggregation.limit(1))
        .`as`("main_pet")

    private fun getSort(criteria: RankingCriteria) =
        Aggregation.sort(Sort.Direction.DESC, criteria.fieldName())
            .and(Sort.Direction.ASC, "user._id")

    private fun RankingCriteria.fieldName(): String = this.name.lowercase()

    private fun KProperty<*>.toSnakeCase(): String =
        this.name
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()

    private fun usedHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    companion object {
        private const val FULL_RANKING_MAX_TIME_SECONDS = 30L
        private const val FULL_RANKING_RESULT_WARN_THRESHOLD = 10_000
    }
}
