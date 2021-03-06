package com.krisbiketeam.smarthomeraspbpi3.common.storage

import androidx.lifecycle.LiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials

interface SecureStorage {
    var firebaseCredentials: FirebaseCredentials
    val firebaseCredentialsLiveData: LiveData<FirebaseCredentials>
    var homeName: String
    val homeNameLiveData: LiveData<String>
    val alarmEnabled: Boolean
    val alarmEnabledLiveData : LiveData<Boolean>

    fun isAuthenticated(): Boolean
}
