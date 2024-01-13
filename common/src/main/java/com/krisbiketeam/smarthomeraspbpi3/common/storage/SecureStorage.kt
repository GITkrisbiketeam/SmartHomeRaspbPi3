package com.krisbiketeam.smarthomeraspbpi3.common.storage

import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NetworkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

internal const val EMAIL_KEY = "secureEmailKey"
internal const val PASSWORD_KEY = "securePasswordKey"
internal const val UID_KEY = "secureUidKey"
internal const val HOME_NAME_KEY = "homeNameKey"
internal const val ALARM_ENABLED_KEY = "alarmEnabledKey"
internal const val REMOTE_LOGGING_LEVEL_KEY = "remoteLoggingLevelKey"
internal const val BME680_STATE_KEY = "bme680StateKey"

interface SecureStorage {

    var firebaseCredentials: FirebaseCredentials
    @ExperimentalCoroutinesApi
    val firebaseCredentialsFlow: Flow<FirebaseCredentials>

    var homeName: String
    @ExperimentalCoroutinesApi
    val homeNameFlow: Flow<String>

    var alarmEnabled: Boolean

    @ExperimentalCoroutinesApi
    val alarmEnabledFlow: Flow<Boolean>

    var remoteLoggingLevel: Int

    @ExperimentalCoroutinesApi
    val remoteLoggingLevelFlow: Flow<Int>

    var bme680State: ByteArray

    var networkState: NetworkState
    var networkIpAddress: String
    var firebaseState: FirebaseState
    val homeState: HomeState

    fun isAuthenticated(): Boolean
}
