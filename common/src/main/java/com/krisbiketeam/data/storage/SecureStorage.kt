package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.SharedPreferences
import com.krisbiketeam.data.auth.FirebaseCredentials
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val SHARED_FILE = "androidThingsExample"
private const val EMAIL_KEY = "secureEmailKey"
private const val PASSWORD_KEY = "securePasswordKey"

interface SecureStorage {
    fun isAuthenticated(): Boolean
    var firebaseCredentials: FirebaseCredentials
    val firebaseCredentialsLiveData: LiveData<FirebaseCredentials>
}

// Todo: implement a encrypted secure storage since this is not secure
class NotSecureStorage(context: Context) : SecureStorage {
    private val sharedPrefs = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)

    override var firebaseCredentials: FirebaseCredentials by sharedPrefs.firebaseCredentials()

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
}



