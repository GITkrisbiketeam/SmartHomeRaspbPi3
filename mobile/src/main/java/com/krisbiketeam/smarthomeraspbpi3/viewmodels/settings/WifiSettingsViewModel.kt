package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.ui.settings.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class WifiSettingsViewModel(val nearByState: NearbyServiceLiveData, wifiManager: WifiManager,
                            connectivityManager: ConnectivityManager) : ViewModel() {
    var ssid: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()

    var ssidList: MutableLiveData<List<String>> = MutableLiveData()

    init {
        wifiManager.startScan()
        Timber.d("init")
        val isNetworkWifiConnected =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } ?: false
        val conInfo = wifiManager.connectionInfo
        val ssidName: String? =
                if (isNetworkWifiConnected && wifiManager.isWifiEnabled && conInfo.frequency / 1000 < 3) {
                    conInfo?.ssid?.trim { '"' == it }
                } else null
        val scanList = wifiManager.scanResults.mapNotNull { scanResult ->
            if (scanResult.frequency / 1000 < 3) {
                scanResult.SSID?.trim { '"' == it }
            } else null
        }
        ssid.value = ssidName
        ssidList.value = scanList

    }

    fun sendData(data: Any) {
        Timber.d("sendData")
        nearByState.value = Pair(MyLiveDataState.CONNECTING, data)
    }

    override fun onCleared() {
        nearByState.onCleared()
        super.onCleared()
    }
}
