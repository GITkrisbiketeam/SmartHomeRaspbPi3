package com.krisbiketeam.smarthomeraspbpi3.ui.setup

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
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
            data?.run {
                val jsonString = String(data)
                Timber.d("Data as String $jsonString")
                val adapter = moshi.adapter(WifiCredentials::class.java)
                val wifiCredentials: WifiCredentials?
                try {
                    wifiCredentials = adapter.fromJson(jsonString)
                    Timber.d("Data as credentials $wifiCredentials")
                    wifiCredentials?.run {
                        addWiFi(wifiCredentials) }
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

        nearbyService = NearbyServiceProvider(activity, moshi)
    }

    fun start() {
        if (!nearbyService.isActive()) {
            nearbyService.dataReceivedListener(dataReceiverListener)
        } else {
            Timber.d("start we are already listening for credentials")
        }
    }

    private fun addWiFi(wifiCredentials: WifiCredentials) {
        // only WPA is supported right now
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = String.format("\"%s\"", wifiCredentials.ssid)
        wifiConfiguration.preSharedKey = String.format("\"%s\"", wifiCredentials.password)

        val existingConfig = wifiManager.configuredNetworks?.firstOrNull{ wifiConfiguration.SSID == it.SSID }
        if (existingConfig != null) {
            Timber.d("This WiFi was already added update it. Existing: $existingConfig new one: $wifiConfiguration")
            existingConfig.preSharedKey = wifiConfiguration.preSharedKey
            val networkId = wifiManager.updateNetwork(existingConfig)
            if (networkId != -1) {
                Timber.d("successful update wifiConfig")
                wifiManager.enableNetwork(networkId, true)
            } else {
                Timber.w("error updating wifiConfig")
            }
        } else {
            Timber.d("This adding new configuration $wifiConfiguration")
            val networkId = wifiManager.addNetwork(wifiConfiguration)
            if (networkId != -1) {
                Timber.d("successful added wifiConfig")
                wifiManager.enableNetwork(networkId, true)
            } else {
                Timber.w("error adding wifiConfig")
            }
        }
    }
}
