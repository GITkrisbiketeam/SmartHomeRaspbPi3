package com.krisbiketeam.smarthomeraspbpi3.common.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val SHARED_FILE = "androidThingsExample"
private const val EMAIL_KEY = "secureEmailKey"
private const val PASSWORD_KEY = "securePasswordKey"
private const val HOME_NAME_KEY = "homeNameKey"
private const val ALARM_ENABLED_KEY = "alarmEnabledKey"

interface SecureStorage {
    fun isAuthenticated(): Boolean
    var firebaseCredentials: FirebaseCredentials
    val firebaseCredentialsLiveData: LiveData<FirebaseCredentials>
    var homeName: String
    val homeNameLiveData: LiveData<String>
    val alarmEnabled: Boolean
    val alarmEnabledLiveData : LiveData<Boolean>
}

// Todo: implement a encrypted secure storage since this is not secure
class NotSecureStorage(context: Context, homeInformationRepository: HomeInformationRepository) : SecureStorage {
    private val sharedPrefs = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    override var firebaseCredentials: FirebaseCredentials by sharedPrefs.firebaseCredentials()

    override var homeName: String by sharedPrefs.homeName()

    override var alarmEnabled: Boolean by prefs.alarmEnabled()

    override val firebaseCredentialsLiveData: LiveData<FirebaseCredentials> =
            object : LiveData<FirebaseCredentials>() {
                private val preferenceChangeListener =
                        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                            value = firebaseCredentials
                        }

                override fun onActive() {
                    super.onActive()
                    value = firebaseCredentials
                    sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
                    super.onInactive()
                }
            }

    override val homeNameLiveData: LiveData<String> =
            object : LiveData<String>() {
                private val preferenceChangeListener =
                        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                            value = homeName
                        }

                override fun onActive() {
                    super.onActive()
                    value = homeName
                    sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
                    super.onInactive()
                }
            }

    override val alarmEnabledLiveData: LiveData<Boolean> =
            object  : LiveData<Boolean>() {
                private val preferenceChangeListener =
                        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                            homeInformationRepository.setHomePreference(ALARM_ENABLED_KEY, alarmEnabled)
                        }

                private val alarmListener: ValueEventListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // A new value has been added, add it to the displayed list
                        val key = dataSnapshot.key
                        val enabled = dataSnapshot.getValue(Boolean::class.java)
                        Timber.d("onDataChange (key=$key)(alarmEnabled=$enabled)")
                        enabled?.let {
                            value = enabled
                            alarmEnabled = enabled
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Timber.e("onCancelled: $databaseError")
                    }
                }
                override fun onActive() {
                    super.onActive()
                    value = alarmEnabled
                    homeInformationRepository.getHomePreference(ALARM_ENABLED_KEY)?.addValueEventListener(alarmListener)
                    prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
                    homeInformationRepository.getHomePreference(ALARM_ENABLED_KEY)?.removeEventListener(alarmListener)
                    super.onInactive()
                }
            }

    override fun isAuthenticated(): Boolean {
        return firebaseCredentials.email.isNotEmpty() && firebaseCredentials.password.isNotEmpty()
    }

    private fun SharedPreferences.firebaseCredentials():
            ReadWriteProperty<Any, FirebaseCredentials> {
        return object : ReadWriteProperty<Any, FirebaseCredentials> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    FirebaseCredentials(
                            getString(EMAIL_KEY, "") ?: "",
                            getString(PASSWORD_KEY, "") ?: "")

            override fun setValue(thisRef: Any, property: KProperty<*>, value: FirebaseCredentials) {
                edit().putString(EMAIL_KEY, value.email).apply()
                edit().putString(PASSWORD_KEY, value.password).apply()
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
}



