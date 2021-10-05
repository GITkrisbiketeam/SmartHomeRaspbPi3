package com.krisbiketeam.smarthomeraspbpi3.viewmodels

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
class NavigationViewModel(secureStorage: SecureStorage, homeRepository: FirebaseHomeInformationRepository) :
        ViewModel() {

    val user: StateFlow<String>
    val home: StateFlow<String>
    val alarm: StateFlow<String>
    val online: StateFlow<String>
    val roomListMenu: StateFlow<List<Room>>

    init {
        Timber.d("init")
        user = secureStorage.firebaseCredentialsFlow.map {
            if (it.uid.isNullOrEmpty()) {
                "Login to Firebase"
            } else {
                homeRepository.startUserToFirebaseConnectionActiveMonitor()
                it.email
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
        home = secureStorage.homeNameFlow.map {
            if (it.isEmpty()) {
                "Setup Home"
            } else {
                it
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
        alarm = secureStorage.alarmEnabledFlow.map {
            if (it) {
                "enabled"
            } else {
                "disabled"
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
        online = secureStorage.homeNameFlow.flatMapLatest {
            combine(homeRepository.isHomeOnlineFlow(), homeRepository.lastHomeOnlineTimeFlow()) { online, lastOnlineTime ->
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
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
        roomListMenu = secureStorage.homeNameFlow.flatMapLatest {
            combine(homeRepository.roomListFlow(), homeRepository.roomListOrderFlow()) { roomList, itemsOrder ->
                mutableListOf<Room>().apply {
                    itemsOrder.forEach { name ->
                        roomList.firstOrNull { name == it.name }?.run(this::add)
                    }
                }
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }
}