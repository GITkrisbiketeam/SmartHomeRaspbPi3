package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.ui.NetworkConnectionListener
import com.krisbiketeam.smarthomeraspbpi3.ui.NetworkConnectionMonitor
import com.krisbiketeam.smarthomeraspbpi3.ui.ThingsActivity
import com.squareup.moshi.Moshi
import timber.log.Timber

class WiFiCredentialsReceiverActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder().build()

    private lateinit var nearbyService: NearbyService

    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private lateinit var wifiManager: WifiManager

    private var savedNetwork: List<WifiConfiguration>? = null

    private val dataReceiverListener = object : NearbyService.DataReceiverListener {
        override fun onDataReceived(data: ByteArray?) {
            Timber.d("Received data: $data")
            data?.let { addWiFi(data) }
        }
    }

    private val networkConnectionListener = object : NetworkConnectionListener {
        override fun onNetworkAvailable(available: Boolean) {
            Timber.d("Received onNetworkAvailable $available")
            if (available) {
                goToMain()
            }
        }
    }

    private fun addWiFi(data: ByteArray) {
        val jsonString = String(data)
        Timber.d("Data as String $jsonString")
        val adapter = moshi.adapter(WifiCredentials::class.java)
        val wifiCredentials = adapter.fromJson(jsonString)
        Timber.d("Data as credentials $wifiCredentials")
        wifiCredentials?.let {
            val wifiConfiguration = WifiConfiguration()
            wifiConfiguration.SSID = it.ssid
            wifiConfiguration.preSharedKey = it.password

            var networkId = -1
            val existingConfig = savedNetwork?.find { wifiConfiguration -> wifiConfiguration.SSID == it.ssid }
            if (existingConfig != null) {
                Timber.d("This WiFi was already added update it existing: $existingConfig new one: $wifiConfiguration")
                existingConfig.preSharedKey = wifiConfiguration.preSharedKey
                networkId = wifiManager.updateNetwork(existingConfig)
                if (networkId != -1) {
                    Timber.d("successful update wifiConfig")
                } else {
                    Timber.w("error updating wifiConfig")
                }
            } else {
                Timber.d("This adding new configuration $wifiConfiguration")
                networkId = wifiManager.addNetwork(wifiConfiguration)
                wifiManager.enableNetwork(networkId, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        //TODO: we need to distinguish nearby connection for WiFi credentials and Firebase credentials
        nearbyService = NearbyServiceProvider(this)
        nearbyService.dataReceivedListener(dataReceiverListener)

        networkConnectionMonitor = NetworkConnectionMonitor(this)

        Timber.e("onCreate isWifiConnectedVal: ${networkConnectionMonitor.isWifiConnectedVal}")
        Timber.e("onCreate isWifiConnected(): ${networkConnectionMonitor.isWifiConnected()}")
        // TODO: wht if user wants to connect to another Wifi???
        if (networkConnectionMonitor.isWifiConnected()) {
            Timber.d("onCreate we are already connected so go to main")
            goToMain()
        }
        savedNetwork = wifiManager.configuredNetworks

    }

    override fun onStart() {
        super.onStart()
        networkConnectionMonitor.startListen(networkConnectionListener)
    }

    override fun onStop() {
        networkConnectionMonitor.stopListen()
        super.onStop()
    }

    private fun goToMain() {
        val intent = Intent(this, ThingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
