package notbe.tmtm.ddanddanserver.domain.model.user

@JvmInline
value class DeviceToken(val value: String) {
    
    init {
        require(value.isNotBlank()) { "DeviceToken cannot be blank" }
    }
    
    fun isValid(): Boolean {
        return value.isNotBlank() && !isDefaultToken()
    }
    
    fun isDefaultToken(): Boolean {
        return value == "deviceToken"
    }
    
    companion object {
        fun of(token: String?): DeviceToken? {
            return if (token.isNullOrBlank()) null else DeviceToken(token)
        }
    }
}

fun String?.toDeviceToken(): DeviceToken? = DeviceToken.of(this)
