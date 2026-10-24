package notbe.tmtm.ddanddanserver.domain.model.user

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.bson.types.ObjectId

class UserTest : FunSpec({
    fun createUser(deviceToken: String?, isAppPushOn: Boolean): User =
        User(
            id = ObjectId(),
            deviceToken = deviceToken.toDeviceToken(),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = isAppPushOn),
        )

    context("pushableDeviceToken") {
        test("푸시가 켜져 있고 토큰이 유효하면 토큰을 반환한다") {
            val user = createUser("valid-token", isAppPushOn = true)

            user.pushableDeviceToken() shouldBe DeviceToken("valid-token")
        }

        test("푸시가 꺼져 있으면 null을 반환한다") {
            val user = createUser("valid-token", isAppPushOn = false)

            user.pushableDeviceToken().shouldBeNull()
        }

        test("토큰이 없으면 null을 반환한다") {
            val user = createUser(null, isAppPushOn = true)

            user.pushableDeviceToken().shouldBeNull()
        }

        test("기본 토큰(deviceToken)이면 null을 반환한다") {
            val user = createUser("deviceToken", isAppPushOn = true)

            user.pushableDeviceToken().shouldBeNull()
        }
    }
})
