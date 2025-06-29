package notbe.tmtm.ddanddanserver.domain.model.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceTokenTest {

    @Test
    fun `유효한 디바이스 토큰으로 생성할 수 있다`() {
        // given
        val validToken = "valid-device-token-123"

        // when
        val deviceToken = DeviceToken(validToken)

        // then
        assertEquals(validToken, deviceToken.value)
        assertTrue(deviceToken.isValid())
        assertFalse(deviceToken.isDefaultToken())
    }

    @Test
    fun `빈 문자열로 디바이스 토큰을 생성하면 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            DeviceToken("")
        }
    }

    @Test
    fun `공백 문자열로 디바이스 토큰을 생성하면 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            DeviceToken("   ")
        }
    }

    @Test
    fun `테스트 토큰을 감지할 수 있다`() {
        // given
        val testToken = DeviceToken("deviceToken")

        // when & then
        assertTrue(testToken.isDefaultToken())
        assertFalse(testToken.isValid())
    }

    @Test
    fun `DeviceToken of 팩토리 메서드는 null-safe하다`() {
        // when & then
        assertNull(DeviceToken.of(null))
        assertNull(DeviceToken.of(""))
        assertNull(DeviceToken.of("   "))
        
        val validToken = DeviceToken.of("valid-token")
        assertEquals("valid-token", validToken?.value)
    }

    @Test
    fun `String 확장 함수 toDeviceToken이 정상 동작한다`() {
        // when & then
        assertNull("".toDeviceToken())
        assertNull(null.let { it?.toDeviceToken() })
        
        val validToken = "valid-token".toDeviceToken()
        assertEquals("valid-token", validToken?.value)
        assertTrue(validToken?.isValid() ?: false)
    }

    @Test
    fun `isValid는 빈 값과 테스트 토큰을 필터링한다`() {
        // given
        val validToken = DeviceToken("valid-token")
        val testToken = DeviceToken("deviceToken")

        // when & then
        assertTrue(validToken.isValid())
        assertFalse(testToken.isValid())
    }
}
