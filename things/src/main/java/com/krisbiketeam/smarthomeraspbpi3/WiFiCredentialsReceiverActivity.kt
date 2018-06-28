package com.krisbiketeam.smarthomeraspbpi3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.squareup.moshi.Moshi
import timber.log.Timber

class WiFiCredentialsReceiverActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder().build()
    private lateinit var nearbyService: NearbyService
    private lateinit var secureStorage: SecureStorage

    private lateinit var wifiManager: WifiManager

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
        nearbyService = NearbyServiceProvider(this)
        nearbyService.dataReceivedListener(dataReceiverListener)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val savedNetwork: MutableList<WifiConfiguration>? = wifiManager.configuredNetworks

    }

    override fun onStart() {
        super.onStart()
        registerReceiver(ConnectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        unregisterReceiver(ConnectivityReceiver)
        super.onStop()
    }

    private fun goToMain() {
        val intent = Intent(this, ThingsActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun addWiFi(fifiCredentials: WifiCredentials) {

        val networkSSID = fifiCredentials.ssid
        val networkPasskey = fifiCredentials.password

        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = networkSSID
        wifiConfiguration.preSharedKey = networkPasskey

        wifiManager.addNetwork(wifiConfiguration)
    }

    companion object ConnectivityReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent?.action) {
                val noConnection = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)

            }
        }
    }
}
