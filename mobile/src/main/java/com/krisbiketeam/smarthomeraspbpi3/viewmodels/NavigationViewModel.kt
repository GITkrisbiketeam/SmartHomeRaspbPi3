package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.storage.SecureStorage
import timber.log.Timber

class NavigationViewModel(secureStorage: SecureStorage) : ViewModel() {
    val user: LiveData<String>
    init {
        Timber.d("init")
        user = Transformations.map(secureStorage.firebaseCredentialsLiveData){
            if(it.email.isEmpty()){
                "Login to Firebase"
            } else {
                it.email
            }
        }
    }
}