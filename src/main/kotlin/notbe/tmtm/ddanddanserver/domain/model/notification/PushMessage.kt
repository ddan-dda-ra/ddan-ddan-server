package notbe.tmtm.ddanddanserver.domain.model.notification

enum class PushMessage(
    val content: String,
) {
    LONG_SLEEPING_USER("\uD83D\uDE22 펫이 기다리고 있어요!"),
    CHECK_CALORIE("\uD83D\uDD25 오늘 몇 칼로리 소모했는지 확인해 볼까요?"),
}
