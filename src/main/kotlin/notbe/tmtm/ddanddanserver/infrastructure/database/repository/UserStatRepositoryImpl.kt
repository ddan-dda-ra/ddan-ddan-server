package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository
import kotlin.reflect.KProperty

@Repository
class UserStatRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : UserStatRepository {
    override fun getRanking(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStatEntity> {
        val agg =
            newAggregation(
                match(
                    Criteria
                        .where(DailyInfo::date.name)
                        .gte(periodType.currentStartDate())
                        .lte(periodType.currentEndDate()),
                ),
                group(DailyInfo::userId.name)
                    .first(DailyInfo::userId.name)
                    .`as`(UserStatEntity::userId.toSnakeCase())
                    .last(DailyInfo::userName.name)
                    .`as`(UserStatEntity::userName.toSnakeCase())
                    .last(DailyInfo::petType.name)
                    .`as`(UserStatEntity::mainPetType.toSnakeCase())
                    .sum(DailyInfo::calorie.name)
                    .`as`(UserStatEntity::totalCalories.toSnakeCase())
                    .sum(Cond.`when`(DailyInfo::purposeAchieved.toSnakeCase()).then(1).otherwise(0))
                    .`as`(UserStatEntity::totalSucceededDays.toSnakeCase()),
                sort(Sort.Direction.DESC, criteria.fieldName())
                    .and(Sort.Direction.ASC, UserStatEntity::userId.toSnakeCase()),
            )
        return mongoTemplate.aggregate(agg, DailyInfo::class.java, UserStatEntity::class.java).mappedResults
    }

    private fun RankingCriteria.fieldName(): String = this.name.lowercase()

    private fun KProperty<*>.toSnakeCase(): String =
        this.name
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
}
