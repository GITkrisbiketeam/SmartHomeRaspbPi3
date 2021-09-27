package com.krisbiketeam.smarthomeraspbpi3.common.storage

import androidx.lifecycle.LiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import kotlinx.coroutines.flow.Flow

internal const val EMAIL_KEY = "secureEmailKey"
internal const val PASSWORD_KEY = "securePasswordKey"
internal const val UID_KEY = "secureUidKey"
internal const val HOME_NAME_KEY = "homeNameKey"
internal const val ALARM_ENABLED_KEY = "alarmEnabledKey"
internal const val BME680_STATE_KEY = "bme680StateKey"

interface SecureStorage {

    var firebaseCredentials: FirebaseCredentials
    val firebaseCredentialsLiveData: LiveData<FirebaseCredentials>
    var homeName: String
    val homeNameLiveData: LiveData<String>
    var alarmEnabled: Boolean
    val alarmEnabledLiveData : LiveData<Boolean>

    var bme680State: ByteArray

    fun isAuthenticated(): Boolean

    val homeNameFlow: Flow<String>
}
