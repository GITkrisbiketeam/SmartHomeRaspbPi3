package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.app.Activity
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.squareup.moshi.Moshi
import timber.log.Timber


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
            data?.run { val jsonString = String(data)
                Timber.d("Data as String $jsonString")
                val adapter = moshi.adapter(FirebaseCredentials::class.java)
                val credentials = adapter.fromJson(jsonString)
                Timber.d("Data as credentials $credentials")
                credentials?.run {
                    secureStorage.firebaseCredentials = credentials
                    gotCredentials()
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
