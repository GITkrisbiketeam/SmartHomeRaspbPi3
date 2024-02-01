package com.krisbiketeam.smarthomeraspbpi3.ui.compose.core.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.text.DateFormat
import java.util.*

@ExperimentalCoroutinesApi
class SmartDrawerViewModel(
    secureStorage: SecureStorage,
    private val homeRepository: FirebaseHomeInformationRepository
) : ViewModel() {

    val uiState: StateFlow<SmartDrawerUiState> =
        combine(
            secureStorage.firebaseCredentialsFlow,
            secureStorage.homeNameFlow,
            secureStorage.alarmEnabledFlow,
            getOnlineStatus(),
            getRoomListMenu()
        ) { firebaseCredentials, homeName, alarmEnabled, onlineStatus, roomList ->

            val user = if (firebaseCredentials.uid.isNullOrEmpty()) {
                "Login to Firebase"
            } else {
                // TODO: should be changed to Flow while setting FirebaseCredentials
                homeRepository.startUserToFirebaseConnectionActiveMonitor()
                firebaseCredentials.email
            }
            val home = homeName.ifEmpty {
                "Setup Home"
            }

            SmartDrawerUiState(
                user = user,
                home = home,
                alarmEnabled = alarmEnabled,
                onlineStatus = onlineStatus,
                roomList = roomList
            )
        }.flowOn(
            Dispatchers.IO
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartDrawerUiState())

    init{
        Timber.i("init")
    }
    override fun onCleared() {
        Timber.i("onCleared")
        super.onCleared()
    }

    private fun getOnlineStatus() = combine(
        homeRepository.isHomeOnlineFlow(),
        homeRepository.lastHomeOnlineTimeFlow()
    ) { online, lastOnlineTime ->
        if (online == true) {
            "Online"
        } else {
            if (lastOnlineTime != null) {
                val formattedDate: String =
                    DateFormat.getDateTimeInstance().format(Date(lastOnlineTime))
                "Offline since: $formattedDate"
            } else {
                "Offline"
            }
        }
    }

    private fun getRoomListMenu() = combine(
        homeRepository.roomListFlow(),
        homeRepository.roomListOrderFlow()
    ) { roomList, itemsOrder ->
        mutableListOf<Room>().apply {
            itemsOrder.forEach { name ->
                roomList.firstOrNull { name == it.name }?.run(this::add)
            }
        }
    }
}