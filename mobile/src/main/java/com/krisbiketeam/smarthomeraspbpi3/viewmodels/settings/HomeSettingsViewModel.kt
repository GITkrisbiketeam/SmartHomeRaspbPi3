package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.ui.settings.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class HomeSettingsViewModel(val nearByState: NearbyServiceLiveData, private val secureStorage: SecureStorage, private val homeInformationRepository: HomeInformationRepository) : ViewModel() {
    var homeName: MutableLiveData<String> = MutableLiveData()
    var remoteHomeSetup: MutableLiveData<Boolean> = MutableLiveData()

    var homeNameList: LiveData<List<String>> = homeInformationRepository.getHomes()

    init {
        Timber.d("init")
        remoteHomeSetup.value = false
    }

    fun setupHomeName(homeName: String) {
        Timber.d("setupHomeName")
        homeInformationRepository.setHomeReference(homeName)
        secureStorage.homeName = homeName
        if (remoteHomeSetup.value == true) {
            nearByState.value = Pair(MyLiveDataState.CONNECTING, homeName)
        } else {
            nearByState.value = Pair(MyLiveDataState.DONE, Unit)
        }
    }

    override fun onCleared() {
        nearByState.onCleared()
        super.onCleared()
    }
}
