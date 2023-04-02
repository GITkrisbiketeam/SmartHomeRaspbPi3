package com.krisbiketeam.smarthomeraspbpi3.common.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.decodeHex
import com.krisbiketeam.smarthomeraspbpi3.common.toHex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val SHARED_PREFERENCES_FILE_NAME = "smarthomeraspbpi3_preferences"
private const val ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "smarthomeraspbpi3_encrypted_preferences"

private const val ENCRYPTED_SHARED_PREFERENCES_MAX_RETRY_COUNT = 3

class SecureStorageImpl(private val context: Context, homeInformationRepository: FirebaseHomeInformationRepository) : SecureStorage {
    private val encryptedSharedPreferences = getEncryptedSharedPreferences()

    private var encryptedSharedPreferencesRetryCount = 0

    override var firebaseCredentials: FirebaseCredentials by encryptedSharedPreferences.firebaseCredentials()

    override var homeName: String by encryptedSharedPreferences.homeName()

    override var alarmEnabled: Boolean by encryptedSharedPreferences.alarmEnabled()

    override var remoteLoggingLevel: Int by encryptedSharedPreferences.remoteLoggingLevel()

    override var bme680State: ByteArray by encryptedSharedPreferences.bme680State()

    @ExperimentalCoroutinesApi
    override val firebaseCredentialsFlow = callbackFlow {
        val preferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    Timber.i("homeNameFlow  changed $key")
                    if (key == EMAIL_KEY || key == PASSWORD_KEY || key == UID_KEY) {
                        this@callbackFlow.trySendBlocking(firebaseCredentials)
                    }
                }
        this@callbackFlow.trySendBlocking(firebaseCredentials)
        Timber.i("firebaseCredentialsFlow  register")
        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        awaitClose {
            Timber.w("firebaseCredentialsFlow  awaitClose")
            encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }.shareIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed(),
            1
    )

    @ExperimentalCoroutinesApi
    override val homeNameFlow = callbackFlow {
        val preferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    Timber.i("homeNameFlow  changed $key")
                    if (key == HOME_NAME_KEY) {
                        this@callbackFlow.trySendBlocking(homeName)
                    }
                }
        this@callbackFlow.trySendBlocking(homeName)
        Timber.i("homeNameFlow  register")
        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        awaitClose {
            Timber.w("homeNameFlow  awaitClose")
            encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }.shareIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed(),
            1
    )

    //TODO refactor this is preference needed here?
    @ExperimentalCoroutinesApi
    override val alarmEnabledFlow = callbackFlow {
        val preferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    Timber.i("alarmEnabledFlow changed $key")
                    if (key == ALARM_ENABLED_KEY) {
                        homeInformationRepository.setHomePreference(ALARM_ENABLED_KEY, alarmEnabled)
                    }
                }

        val alarmListener: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key
                val enabled = dataSnapshot.getValue(Boolean::class.java)
                Timber.d("alarmEnabledFlow onDataChange (key=$key)(alarmEnabled=$enabled)")
                if (enabled != null) {
                    this@callbackFlow.trySendBlocking(enabled)
                    alarmEnabled = enabled
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.e("alarmEnabledFlow onCancelled: $databaseError")
            }
        }

        this@callbackFlow.trySendBlocking(alarmEnabled)
        homeInformationRepository.getHomePreference(ALARM_ENABLED_KEY)?.addValueEventListener(alarmListener)
        Timber.i("alarmEnabledFlow  register")
        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        awaitClose {
            Timber.w("alarmEnabledFlow  awaitClose")
            encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
            homeInformationRepository.getHomePreference(ALARM_ENABLED_KEY)?.removeEventListener(alarmListener)
        }
    }.shareIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed(),
            1
    )

    @ExperimentalCoroutinesApi
    override val remoteLoggingLevelFlow: Flow<Int> = callbackFlow {
        val preferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    Timber.i("remoteLoggingLevelFlow changed $key")
                    if (key == REMOTE_LOGGING_LEVEL_KEY) {
                        homeInformationRepository.setHomePreference(REMOTE_LOGGING_LEVEL_KEY, remoteLoggingLevel)
                    }
                }

        val remoteLoggingLevelListener: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key
                Timber.d("remoteLoggingLevelFlow onDataChange (key=$key)(dataSnapshot=$dataSnapshot)")
                val level = dataSnapshot.getValue(Int::class.java)
                Timber.d("remoteLoggingLevelFlow onDataChange (key=$key)(remoteLoggingLevel=$level)")
                if (level != null) {
                    this@callbackFlow.trySendBlocking(level)
                    remoteLoggingLevel = level
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.e("remoteLoggingLevelFlow onCancelled: $databaseError")
            }
        }

        this@callbackFlow.trySendBlocking(remoteLoggingLevel)
        homeInformationRepository.getHomePreference(REMOTE_LOGGING_LEVEL_KEY)?.addValueEventListener(remoteLoggingLevelListener)
        Timber.e("remoteLoggingLevelFlow  register")
        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        awaitClose {
            Timber.w("remoteLoggingLevelFlow  awaitClose")
            encryptedSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
            homeInformationRepository.getHomePreference(REMOTE_LOGGING_LEVEL_KEY)?.removeEventListener(remoteLoggingLevelListener)
        }
    }.shareIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed(),
            1
    )


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
                            // additionally delete encrypted SharedPref file
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



