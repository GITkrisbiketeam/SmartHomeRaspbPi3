package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.ui.ThingsActivity
import com.squareup.moshi.Moshi
import timber.log.Timber


class FirebaseCredentialsReceiverActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder().build()
    private lateinit var nearbyService: NearbyService
    private lateinit var secureStorage: SecureStorage

    private val dataReceiverListener = object : NearbyService.DataReceiverListener {
        override fun onDataReceived(data: ByteArray?) {
            Timber.d("Received data: $data")
            data?.let { saveCredentials(data) }
        }
    }

    private fun saveCredentials(data: ByteArray) {
        val jsonString = String(data)
        Timber.d("Data as String $jsonString")
        val adapter = moshi.adapter(FirebaseCredentials::class.java)
        val credentials = adapter.fromJson(jsonString)
        Timber.d("Data as credentials $credentials")
        credentials?.let {
            secureStorage.saveFirebaseCredentials(credentials)
            goToMain()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        secureStorage = NotSecureStorage(this)

        //TODO: we need to distinguish nearby connection for WiFi credentials and Firebase credentials
        nearbyService = NearbyServiceProvider(this)
        nearbyService.dataReceivedListener(dataReceiverListener)
    }

    private fun goToMain() {
        val intent = Intent(this, ThingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
