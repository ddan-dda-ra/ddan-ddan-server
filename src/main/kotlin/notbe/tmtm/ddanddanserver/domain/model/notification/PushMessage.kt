package notbe.tmtm.ddanddanserver.domain.model.notification

object PushMessage {
    
    // 정적 메시지
    const val LONG_SLEEPING_USER = "😢 펫이 기다리고 있어요!"
    const val CHECK_CALORIE = "🔥 오늘 몇 칼로리 소모했는지 확인해 볼까요?"
    const val RANKING_DOWN = "📉 순위가 떨어졌어요!"
    
    // 동적 메시지 생성 함수
    fun weeklyRanking(calories: Int): String {
        return "📈 이번 주 칼로리 소모 1위는? ${calories}칼로리를 소모했대요!"
    }
}
