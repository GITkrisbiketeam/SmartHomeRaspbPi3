package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import timber.log.Timber

class NavigationViewModel(
        secureStorage: SecureStorage
) : ViewModel() {

    val user: LiveData<String>
    val home: LiveData<String>

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
    }
}