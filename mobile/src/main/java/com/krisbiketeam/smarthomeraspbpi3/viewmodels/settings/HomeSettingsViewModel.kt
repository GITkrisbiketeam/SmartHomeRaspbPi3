package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.ui.settings.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class HomeSettingsViewModel(val nearByState: NearbyServiceLiveData) : ViewModel() {
    var homeName: MutableLiveData<String> = MutableLiveData()
    var remoteHomeSetup: MutableLiveData<Boolean> = MutableLiveData()

    init {
        Timber.d("init")
        remoteHomeSetup.value = false
    }

    fun setupHomeName(data: String) {
        Timber.d("setupHomeName")
        FirebaseHomeInformationRepository.setHomeReference(data)
        if (remoteHomeSetup.value == true) {
            nearByState.value = Pair(MyLiveDataState.CONNECTING, data)
        } else {
            nearByState.value = Pair(MyLiveDataState.DONE, Unit)
        }
    }

    override fun onCleared() {
        nearByState.onCleared()
        super.onCleared()
    }
}
