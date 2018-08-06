package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.ui.NetworkConnectionMonitor
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.io.IOException

class WiFiCredentialsReceiverManager(activity: Activity, networkConnectionMonitor: NetworkConnectionMonitor) {

    private val moshi = Moshi.Builder().build()

    private var nearbyService: NearbyService

    private var wifiManager: WifiManager = activity.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val dataReceiverListener = object : NearbyService.DataReceiverListener {
        override fun onDataReceived(data: ByteArray?) {
            Timber.d("Received data: $data")
            data?.let {
                val jsonString = String(it)
                Timber.d("Data as String $jsonString")
                val adapter = moshi.adapter(WifiCredentials::class.java)
                var wifiCredentials: WifiCredentials? = null
                try {
                    wifiCredentials = adapter.fromJson(jsonString)
                    Timber.d("Data as credentials $wifiCredentials")
                    wifiCredentials?.let { addWiFi(it) }
                } catch (e: IOException) {
                    Timber.d("Received Data could not be cast to WiFiCredentials")
                }
            }
        }
    }

    init {
        wifiManager.configuredNetworks?.forEach {
            Timber.e("init savedNetwork ssid: ${it.SSID}")
        }

        Timber.e("init isWifiConnectedVal: ${networkConnectionMonitor.isWifiConnectedVal}")
        Timber.e("init isWifiConnected(): ${networkConnectionMonitor.isWifiConnected()}")

        if (networkConnectionMonitor.isWifiConnected()) {
            Timber.d("init we are already connected")
        }

        nearbyService = NearbyServiceProvider(activity)
    }

    fun start() {
        if (!nearbyService.isActive()) {
            nearbyService.dataReceivedListener(dataReceiverListener)
        } else {
            Timber.d("start we are already listening for credentials")
        }
    }

    private fun addWiFi(wifiCredentials: WifiCredentials) {
        wifiCredentials.let {
            // only WPA is supported right now
            val wifiConfiguration = WifiConfiguration()
            wifiConfiguration.SSID = String.format("\"%s\"", it.ssid)
            wifiConfiguration.preSharedKey = String.format("\"%s\"", it.password)

            var networkId = -1
            val existingConfig = wifiManager.configuredNetworks?.firstOrNull{ wifiConfiguration.SSID == it.SSID }
            if (existingConfig != null) {
                Timber.d("This WiFi was already added update it. Existing: $existingConfig new one: $wifiConfiguration")
                existingConfig.preSharedKey = wifiConfiguration.preSharedKey
                networkId = wifiManager.updateNetwork(existingConfig)
                if (networkId != -1) {
                    Timber.d("successful update wifiConfig")
                    wifiManager.enableNetwork(networkId, true)
                } else {
                    Timber.w("error updating wifiConfig")
                }
            } else {
                Timber.d("This adding new configuration $wifiConfiguration")
                networkId = wifiManager.addNetwork(wifiConfiguration)
                if (networkId != -1) {
                    Timber.d("successful added wifiConfig")
                    wifiManager.enableNetwork(networkId, true)
                } else {
                    Timber.w("error adding wifiConfig")
                }
            }
        }
    }
}