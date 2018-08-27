package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.data.nearby.WifiSettingsState
import com.krisbiketeam.smarthomeraspbpi3.ui.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class WifiSettingsViewModel(nearbyService: NearbyService) : ViewModel() {
    var ssid: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()
    val state: NearbyServiceLiveData = NearbyServiceLiveData(nearbyService)

    init{
        Timber.d("init")
    }

    fun sendData(data: Any) {
        Timber.d("sendData")
        state.value = Pair(WifiSettingsState.CONNECTING, data)
    }

    override fun onCleared() {
        state.onCleared()
        super.onCleared()
    }
}
