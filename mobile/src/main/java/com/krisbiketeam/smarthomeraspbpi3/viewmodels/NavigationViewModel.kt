package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
        home = Transformations.map(secureStorage.homeNameLiveData) {
            if (it.isEmpty()) {
                "Setup Home"
            } else {
                it
            }
        }
        alarm = Transformations.map(secureStorage.alarmEnabledLiveData) {
            if (it) {
                "enabled"
            } else {
                "disabled"
            }
        }
        online = Transformations.switchMap(secureStorage.homeNameLiveData) {
            Transformations.switchMap(homeRepository.isHomeOnline()) {
                if (it == true) {
                    MutableLiveData("Online")
                } else {
                    Transformations.map(homeRepository.lastHomeOnlineTime()) { time ->
                        if (time != null) {

                            val formattedDate: String =
                                    DateFormat.getDateTimeInstance().format(Date(time))
                            "Offline since: $formattedDate"
                        } else {
                            "Offline"
                        }
                    }
                }
            }
        }
        roomListMenu = secureStorage.homeNameFlow().flatMapLatest {
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