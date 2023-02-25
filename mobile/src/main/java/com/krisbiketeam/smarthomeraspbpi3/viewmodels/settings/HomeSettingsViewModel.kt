package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.ui.settings.WifiSettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class HomeSettingsViewModel(val nearByState: NearbyServiceLiveData, private val secureStorage: SecureStorage, private val homeInformationRepository: FirebaseHomeInformationRepository) : ViewModel() {
    val homeName: MutableStateFlow<String> = MutableStateFlow(secureStorage.homeName)
    val remoteHomeSetup: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val homeNameList: StateFlow<List<String>> = homeInformationRepository.getHomesFLow().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setupHomeName(homeName: String) {
        Timber.d("setupHomeName")
        if (!homeNameList.value.contains(homeName)) {
            homeInformationRepository.addHomeToList(homeName)
        }
        secureStorage.homeName = homeName
        homeInformationRepository.setHomeReference(homeName)
        if (remoteHomeSetup.value) {
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
