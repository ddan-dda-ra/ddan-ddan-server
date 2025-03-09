package notbe.tmtm.ddanddanserver.domain.model.notification

enum class PushMessage(
    val content: String,
) {
    LONG_SLEEPING_USER("\uD83D\uDE22 펫이 기다리고 있어요!"),
}
