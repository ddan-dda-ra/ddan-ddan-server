package notbe.tmtm.ddanddanserver.domain.model.notification

enum class PushMessage(
    val message: String,
) {
    LONG_SLEEPING_USER("\uD83D\uDE22 펫이 기다리고 있어요!"),
    CHECK_CALORIE("\uD83D\uDD25 오늘 몇 칼로리 소모했는지 확인해 볼까요?"),
    WEEKLY_RANKING("\uD83D\uDCC8 이번 주 칼로리 소모 1위는?"),
    RANKING_UP("\uD83D\uDCC8 랭킹이 %d칸 상승했어요!"),
    RANKING_DOWN("\uD83D\uDCC8 랭킹이 %d칸 하락했어요!"),
}
