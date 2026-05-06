package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

@Repository
class StatsAdminRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : StatsAdminRepository {
    override fun countAllUsers(): Long = mongoTemplate.count(Query(), User::class.java)

    override fun countAllPets(): Long = mongoTemplate.count(Query(), Pet::class.java)

    override fun countNewUsersBetween(from: LocalDate, to: LocalDate): Long {
        val fromId = startOfDayObjectId(from)
        val toId = startOfDayObjectId(to.plusDays(1))
        val query = Query(Criteria.where("_id").gte(fromId).lt(toId))
        return mongoTemplate.count(query, User::class.java)
    }

    override fun countDistinctActiveUsersBetween(from: LocalDate, to: LocalDate): Long {
        // daily_calories.date는 LocalDate(시간 정보 없음) 타입으로 mongo에 저장되어
        // 쿼리/저장 모두 동일 직렬화를 거치므로 KST 별도 변환이 불필요.
        // getSignupSeries는 _id(UTC ObjectId timestamp) 기반이라 KST 변환이 필요한 것과 다른 케이스.
        val agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("date").gte(from).lte(to)),
            Aggregation.group("userId"),
            Aggregation.count().`as`("count"),
        )
        val result = mongoTemplate
            .aggregate(agg, "daily_calories", CountAggregateResult::class.java)
            .uniqueMappedResult
        return result?.count ?: 0L
    }

    override fun groupPetsByType(): List<StatsPetTypeCount> {
        val agg = Aggregation.newAggregation(
            Aggregation.group("type").count().`as`("count"),
            Aggregation.sort(Sort.Direction.DESC, "count"),
        )
        val results = mongoTemplate
            .aggregate(agg, "pets", PetTypeAggregateResult::class.java)
            .mappedResults
        return results.mapNotNull { it.toDomainOrNull() }
    }

    override fun getSignupSeries(from: LocalDate, to: LocalDate): List<StatsDailyCount> {
        val fromId = startOfDayObjectId(from)
        val toId = startOfDayObjectId(to.plusDays(1))

        val agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("_id").gte(fromId).lt(toId)),
            // _id(ObjectId)의 timestamp를 KST 기준 날짜 문자열로 변환
            AggregationOperation {
                Document(
                    "\$project",
                    Document(
                        "date",
                        Document(
                            "\$dateToString",
                            Document("format", "%Y-%m-%d")
                                .append("date", Document("\$toDate", "\$_id"))
                                .append("timezone", KST),
                        ),
                    ),
                )
            },
            Aggregation.group("date").count().`as`("count"),
            Aggregation.sort(Sort.Direction.ASC, "_id"),
        )
        val raw = mongoTemplate
            .aggregate(agg, "users", DailyAggregateResult::class.java)
            .mappedResults
        val byDate = raw.associate { it.id to it.count }
        // gap-filling: from~to 모든 날짜를 포함, 누락일은 0
        return generateDateRange(from, to).map { d ->
            val key = d.toString()
            StatsDailyCount(date = key, count = byDate[key] ?: 0L)
        }
    }

    private fun startOfDayObjectId(date: LocalDate): ObjectId {
        val instant = date.atStartOfDay(ZoneId.of(KST)).toInstant()
        return ObjectId(Date.from(instant))
    }

    private fun generateDateRange(from: LocalDate, to: LocalDate): List<LocalDate> {
        val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
        return (0 until days).map { from.plusDays(it.toLong()) }
    }

    data class CountAggregateResult(val count: Long)

    data class PetTypeAggregateResult(
        val id: String?,
        val count: Long,
    ) {
        fun toDomainOrNull(): StatsPetTypeCount? {
            val typeName = id ?: return null
            return StatsPetTypeCount(type = typeName, count = count)
        }
    }

    data class DailyAggregateResult(
        val id: String,
        val count: Long,
    )

    private companion object {
        const val KST = "Asia/Seoul"
    }
}
