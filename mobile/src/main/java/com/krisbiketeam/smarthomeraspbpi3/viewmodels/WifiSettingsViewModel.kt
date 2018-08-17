package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.ui.WifiSettingsFragment
/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class WifiSettingsViewModel : ViewModel() {
    var ssid: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()
    var progressBar: MutableLiveData<Boolean> = MutableLiveData()

    init {
        progressBar.value = false
    }
}
