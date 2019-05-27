package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.app.Activity
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.io.IOException


class FirebaseCredentialsReceiverManager(activity: Activity, gotCredentials: () -> Unit) {

    private val moshi = Moshi.Builder().build()
    private var nearbyService: NearbyService
    private var secureStorage: SecureStorage

    init {
        Timber.d("onCreate")

        secureStorage = NotSecureStorage(activity)

        //TODO: we need to distinguish nearby connection for WiFi credentials and Firebase credentials
        nearbyService = NearbyServiceProvider(activity, moshi)
    }

    private val dataReceiverListener = object : NearbyService.DataReceiverListener {
        override fun onDataReceived(data: ByteArray?) {
            Timber.d("Received data: $data")
            data?.run {
                val jsonString = String(data)
                Timber.d("Data as String $jsonString")
                val adapter = moshi.adapter(FirebaseCredentials::class.java)
                val credentials: FirebaseCredentials?
                try {
                    credentials = adapter.fromJson(jsonString)
                    Timber.d("Data as credentials $credentials")
                    credentials?.run {
                        secureStorage.firebaseCredentials = credentials
                        gotCredentials()
                    }
                } catch (e: IOException) {
                    Timber.d("Received Data could not be cast to FirebaseCredentials")
                }
            }
        }
    }

    fun start() {
        if (!nearbyService.isActive()) {
            nearbyService.dataReceivedListener(dataReceiverListener)
        } else {
            Timber.d("start we are already listening for credentials")
        }
    }
}
