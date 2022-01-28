package com.krisbiketeam.smarthomeraspbpi3.common.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.decodeHex
import com.krisbiketeam.smarthomeraspbpi3.common.toHex
import kotlinx.coroutines.flow.Flow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val SHARED_FILE = "androidThingsExample"

// Todo: implement a encrypted secure storage since this is not secure
class NotSecureStorage(context: Context, homeInformationRepository: FirebaseHomeInformationRepository) : SecureStorage {
    private val sharedPrefs = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    override var firebaseCredentials: FirebaseCredentials by sharedPrefs.firebaseCredentials()

    override var homeName: String by sharedPrefs.homeName()

    override var alarmEnabled: Boolean by prefs.alarmEnabled()

    override var remoteLoggingLevel: Int by prefs.remoteLoggingLevel()

    override var bme680State: ByteArray by prefs.bme680State()

    override fun isAuthenticated(): Boolean {
        return firebaseCredentials.email.isNotEmpty() && firebaseCredentials.password.isNotEmpty() && !firebaseCredentials.uid.isNullOrEmpty()
    }

    override val homeNameFlow: Flow<String> get() = TODO("Not yet implemented")

    override val firebaseCredentialsFlow: Flow<FirebaseCredentials> get() = TODO("Not yet implemented")

    override val alarmEnabledFlow: Flow<Boolean> get() = TODO("Not yet implemented")

    override val remoteLoggingLevelFlow: Flow<Int> get() = TODO("Not yet implemented")

    private fun SharedPreferences.firebaseCredentials():
            ReadWriteProperty<Any, FirebaseCredentials> {
        return object : ReadWriteProperty<Any, FirebaseCredentials> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    FirebaseCredentials(
                            getString(EMAIL_KEY, "") ?: "",
                            getString(PASSWORD_KEY, "") ?: "",
                            getString(UID_KEY, "") ?: "")

            override fun setValue(thisRef: Any, property: KProperty<*>, value: FirebaseCredentials) {
                edit().putString(EMAIL_KEY, value.email).apply()
                edit().putString(PASSWORD_KEY, value.password).apply()
                edit().putString(UID_KEY, value.uid).apply()
            }
        }
    }

    private fun SharedPreferences.homeName():
            ReadWriteProperty<Any, String> {
        return object : ReadWriteProperty<Any, String> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getString(HOME_NAME_KEY, "") ?: ""

            override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
                edit().putString(HOME_NAME_KEY, value).apply()
            }
        }
    }

    private fun SharedPreferences.alarmEnabled():
            ReadWriteProperty<Any, Boolean> {
        return object : ReadWriteProperty<Any, Boolean> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getBoolean(ALARM_ENABLED_KEY, true)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
                edit().putBoolean(ALARM_ENABLED_KEY, value).apply()
            }
        }
    }

    private fun SharedPreferences.remoteLoggingLevel():
            ReadWriteProperty<Any, Int> {
        return object : ReadWriteProperty<Any, Int> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getInt(REMOTE_LOGGING_LEVEL_KEY, Int.MAX_VALUE)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
                edit().putInt(REMOTE_LOGGING_LEVEL_KEY, value).apply()
            }
        }
    }

    private fun SharedPreferences.bme680State():
            ReadWriteProperty<Any, ByteArray> {
        return object : ReadWriteProperty<Any, ByteArray> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getString(BME680_STATE_KEY, "").decodeHex()

            override fun setValue(thisRef: Any, property: KProperty<*>, value: ByteArray) {
                edit { putString(BME680_STATE_KEY, value.toHex()) }
            }
        }
    }
}



