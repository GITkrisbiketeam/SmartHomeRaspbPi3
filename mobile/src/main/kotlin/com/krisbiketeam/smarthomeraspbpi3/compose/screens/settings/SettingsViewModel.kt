package com.krisbiketeam.smarthomeraspbpi3.compose.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.startWith
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class SettingsViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SettingsUiState> = combine(
        secureStorage.alarmEnabledFlow,
        homeRepository.restartAppFlow().onStart { emit(false) },
    ) { alarmEnabled, appRestarting ->
        Timber.i("alarmEnabled:$alarmEnabled appRestarting:$appRestarting")
        SettingsUiState(alarmEnabled, appRestarting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = SettingsUiState()
    )

    fun setAlarmState(enable: Boolean){
        secureStorage.alarmEnabled = enable
    }

    fun resetRPi3App() {
        Timber.d("onPreferenceTreeClick restart Rpi Things App")
        homeRepository.setResetAppFlag()
    }
}

data class SettingsUiState(val alarmEnabled: Boolean = false, val appRestarting: Boolean = false)