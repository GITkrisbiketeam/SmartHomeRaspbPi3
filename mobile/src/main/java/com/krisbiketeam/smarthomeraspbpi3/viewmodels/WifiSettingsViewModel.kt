package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.ui.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class WifiSettingsViewModel(val nearByState: NearbyServiceLiveData) : ViewModel() {
    var ssid: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()

    init{
        Timber.d("init")
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
