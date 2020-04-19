package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import timber.log.Timber
import java.text.DateFormat
import java.util.*

class NavigationViewModel(secureStorage: SecureStorage, homeRepository: FirebaseHomeInformationRepository) :
        ViewModel() {

    val user: LiveData<String>
    val home: LiveData<String>
    val alarm: LiveData<String>
    val online: LiveData<String>

    init {
        Timber.d("init")
        user = Transformations.map(secureStorage.firebaseCredentialsLiveData) {
            if (it.email.isEmpty()) {
                "Login to Firebase"
            } else {
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
    }
}