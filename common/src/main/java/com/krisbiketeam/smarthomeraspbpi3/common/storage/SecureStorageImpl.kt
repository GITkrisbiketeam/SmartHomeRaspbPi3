package com.krisbiketeam.smarthomeraspbpi3.common.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val SHARED_PREFERENCES_FILE_NAME = "smarthomeraspbpi3_preferences"
private const val ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "smarthomeraspbpi3_encrypted_preferences"

private const val ENCRYPTED_SHARED_PREFERENCES_MAX_RETRY_COUNT = 3

private const val EMAIL_KEY = "secureEmailKey"
private const val PASSWORD_KEY = "securePasswordKey"
private const val UID_KEY = "secureUidKey"
private const val HOME_NAME_KEY = "homeNameKey"
private const val ALARM_ENABLED_KEY = "alarmEnabledKey"

class SecureStorageImpl(private val context: Context, homeInformationRepository: FirebaseHomeInformationRepository) : SecureStorage {
    private val encryptedSharedPreferences = getEncryptedSharedPreferences()

    private var encryptedSharedPreferencesRetryCount = 0

    override var firebaseCredentials: FirebaseCredentials by encryptedSharedPreferences.firebaseCredentials()

    override var homeName: String by encryptedSharedPreferences.homeName()

    override var alarmEnabled: Boolean by encryptedSharedPreferences.alarmEnabled()

    override val firebaseCredentialsLiveData: LiveData<FirebaseCredentials> =
            object : LiveData<FirebaseCredentials>() {
                private val preferenceChangeListener =
                        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                            value = firebaseCredentials
                        }

                override fun onActive() {
                    super.onActive()
                    value = firebaseCredentials
                    encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
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
                    encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
                    super.onInactive()
                }
            }

    override val alarmEnabledLiveData: LiveData<Boolean> =
            object : LiveData<Boolean>() {
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
                    encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
                }

                override fun onInactive() {
                    encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
                    homeInformationRepository.getHomePreference(ALARM_ENABLED_KEY)?.removeEventListener(alarmListener)
                    super.onInactive()
                }
            }

    override fun isAuthenticated(): Boolean {
        return firebaseCredentials.email.isNotEmpty() && firebaseCredentials.password.isNotEmpty() && !firebaseCredentials.uid.isNullOrEmpty()
    }

    private fun SharedPreferences.firebaseCredentials():
            ReadWriteProperty<Any, FirebaseCredentials> {
        return object : ReadWriteProperty<Any, FirebaseCredentials> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    FirebaseCredentials(
                            getString(EMAIL_KEY, "") ?: "",
                            getString(PASSWORD_KEY, "") ?: "",
                            getString(UID_KEY, "") ?: "")

            override fun setValue(thisRef: Any, property: KProperty<*>, value: FirebaseCredentials) {
                edit {
                    putString(EMAIL_KEY, value.email)
                    putString(PASSWORD_KEY, value.password)
                    putString(UID_KEY, value.uid)
                }
            }
        }
    }

    private fun SharedPreferences.homeName():
            ReadWriteProperty<Any, String> {
        return object : ReadWriteProperty<Any, String> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getString(HOME_NAME_KEY, "") ?: ""

            override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
                edit { putString(HOME_NAME_KEY, value) }
            }
        }
    }

    private fun SharedPreferences.alarmEnabled():
            ReadWriteProperty<Any, Boolean> {
        return object : ReadWriteProperty<Any, Boolean> {
            override fun getValue(thisRef: Any, property: KProperty<*>) =
                    getBoolean(ALARM_ENABLED_KEY, true)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
                edit { putBoolean(ALARM_ENABLED_KEY, value) }
            }
        }
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(context, ENCRYPTED_SHARED_PREFERENCES_FILE_NAME,
                    MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        } catch (e: java.lang.Exception) {
            when (e) {
                is KeyStoreException,                   // If a master key is found but unusable.
                is GeneralSecurityException,            // If cannot read an existing keyset or generate a new one.
                is IOException -> {                     // If a keyset is found but unusable.
                    if (++encryptedSharedPreferencesRetryCount <= ENCRYPTED_SHARED_PREFERENCES_MAX_RETRY_COUNT) {
                        Timber.e("EncryptedSharedPreferences instantiation error, retry $e")
                        if (e !is KeyStoreException && encryptedSharedPreferencesRetryCount == ENCRYPTED_SHARED_PREFERENCES_MAX_RETRY_COUNT) {
                            // clear ENCRYPTED_SHARED_PREFERENCES_FILE_NAME
                            context.getSharedPreferences(ENCRYPTED_SHARED_PREFERENCES_FILE_NAME, 0)
                                    .let { prefs ->
                                        prefs.edit {
                                            prefs.all.keys.forEach(this::remove)
                                        }
                                    }
                            // additionally delete encrepted SharedPref file
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                context.deleteSharedPreferences(ENCRYPTED_SHARED_PREFERENCES_FILE_NAME)
                            }
                        }
                        // add some delay before trying to get EncryptedSharedPreferences again
                        Thread.sleep((100L..1000L).random())
                        getEncryptedSharedPreferences()
                    } else {
                        Timber.e("Permanently failed to instantiate EncryptedSharedPreferences $e")
                        context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
                    }
                }
                else -> {
                    Timber.e("Permanently failed to instantiate EncryptedSharedPreferences $e")
                    context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }
}


