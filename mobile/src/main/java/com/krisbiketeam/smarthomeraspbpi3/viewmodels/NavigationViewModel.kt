package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.DateFormat
import java.util.*

@ExperimentalCoroutinesApi
class NavigationViewModel(secureStorage: SecureStorage, homeRepository: FirebaseHomeInformationRepository) :
        ViewModel() {

    val user: LiveData<String>
    val home: LiveData<String>
    val alarm: LiveData<String>
    val online: LiveData<String>
    val roomListMenu: LiveData<List<Room>>

    init {
        Timber.d("init")
        user = Transformations.map(secureStorage.firebaseCredentialsLiveData) {
            if (it.uid.isNullOrEmpty()) {
                "Login to Firebase"
            } else {
                homeRepository.startUserToFirebaseConnectionActiveMonitor()
                it.email
            }
        }
        home = secureStorage.homeNameFlow.map {
            if (it.isEmpty()) {
                "Setup Home"
            } else {
                it
            }
        }.asLiveData(Dispatchers.IO)
        alarm = Transformations.map(secureStorage.alarmEnabledLiveData) {
            if (it) {
                "enabled"
            } else {
                "disabled"
            }
        }
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
        }.asLiveData(Dispatchers.Default)
        roomListMenu = secureStorage.homeNameFlow.flatMapLatest {
            combine(homeRepository.roomListFlow(), homeRepository.roomListOrderFlow()) { roomList, itemsOrder ->
                mutableListOf<Room>().apply {
                    itemsOrder.forEach { name ->
                        roomList.firstOrNull { name == it.name }?.run(this::add)
                    }
                }
            }
        }.asLiveData(Dispatchers.Default)
    }
}