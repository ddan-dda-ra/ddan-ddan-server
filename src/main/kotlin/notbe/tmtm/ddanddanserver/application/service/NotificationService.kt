package notbe.tmtm.ddanddanserver.application.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.domain.model.notification.PushMessage
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val fcmClient: FirebaseMessaging,
) {
    @Transactional(readOnly = true)
    fun notifyCheckCalorie() {
        val allUsers = userRepository.findAll()
            .filter { it.setting.isAppPushOn }
            .filter { it.deviceToken.isNullOrEmpty().not() }
        
        sendPushToAllUsers(allUsers.map { it.deviceToken!! }, PushMessage.CHECK_CALORIE.content, RoutingView.MAIN)
    }

    @Transactional(readOnly = true)
    fun notifySleepingUsers() {
        val sleepingUsers = userRepository.findAll()
            .filter { it.deviceToken.isNullOrEmpty().not() }
        
        sleepingUsers.forEach { user ->
            sendPushToUser(user.deviceToken!!, "잠들어있는 동물을 확인해주세요!", RoutingView.MAIN)
        }
    }

    @Transactional(readOnly = true)
    fun notifyWeeklyRanking(totalCalories: Int) {
        val allUsers = userRepository.findAll()
            .filter { it.setting.isAppPushOn }
            .filter { it.deviceToken.isNullOrEmpty().not() }
        
        sendPushToAllUsers(
            allUsers.map { it.deviceToken!! },
            "이번 주 1등의 칼로리는 ${totalCalories}칼로리입니다!",
            RoutingView.MAIN
        )
    }

    @Transactional(readOnly = true)
    fun notifyRankingDiff() {
        val allUsers = userRepository.findAll()
            .filter { it.setting.isAppPushOn }
            .filter { it.deviceToken.isNullOrEmpty().not() }
        
        allUsers.forEach { user ->
            // 랭킹 차이 계산 로직은 나중에 구현
            sendPushToUser(user.deviceToken!!, "랭킹이 변동되었습니다!", RoutingView.MAIN)
        }
    }

    private fun sendPushToAllUsers(deviceTokens: List<String>, message: String, routingView: RoutingView) {
        if (deviceTokens.isEmpty()) return
        
        val requests = deviceTokens
            .filter { it != "deviceToken" }
            .map { token ->
                Message
                    .builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setBody(message).build())
                    .putData("routingView", routingView.name)
                    .build()
            }
        
        fcmClient.sendEach(requests)
    }

    private fun sendPushToUser(deviceToken: String, message: String, routingView: RoutingView) {
        if (deviceToken == "deviceToken") return
        
        val request = Message
            .builder()
            .setToken(deviceToken)
            .setNotification(Notification.builder().setBody(message).build())
            .putData("routingView", routingView.name)
            .build()

        fcmClient.send(request)
    }
}