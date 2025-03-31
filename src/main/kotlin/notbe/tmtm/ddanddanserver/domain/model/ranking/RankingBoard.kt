package notbe.tmtm.ddanddanserver.domain.model.ranking

class RankingBoard(
    val id: PeriodType,
    val ranking: List<Pair<Int, UserStat>>
) {
    companion object {
        fun of(id: PeriodType, ranking: List<UserStat>): RankingBoard {
            return RankingBoard(
                id = id,
                ranking = ranking.mapIndexed { index, userStat -> index + 1 to userStat }
            )
        }
    }
}
