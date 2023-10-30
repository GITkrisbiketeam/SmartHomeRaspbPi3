package com.krisbiketeam.smarthomeraspbpi3.common.ble

import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage

class ThingsBleStateProvider(private val secureStorage: SecureStorage) {

    fun readData(request: ReadCharacteristicRequest): ReadCharacteristicData {
        return when (request) {
            is ReadFirebaseLoginRequest -> ReadFirebaseLoginData(secureStorage.firebaseCredentials.email)
            is ReadFirebaseStateRequest -> {
                val state = when {
                    secureStorage.isAuthenticated() -> {
                        FirebaseState.LOGGED
                    }

                    secureStorage.firebaseCredentials.email.isNotEmpty()
                            && secureStorage.firebaseCredentials.password.isNotEmpty() -> {
                        FirebaseState.SET
                    }

                    else -> {
                        FirebaseState.NONE
                    }
                }
                ReadFirebaseStateData(state)
            }

            is ReadHomeNameRequest -> ReadHomeNameData(secureStorage.homeName)
            is ReadHomeStateRequest -> ReadHomeStateData(if (secureStorage.homeName.isNotEmpty()) HomeState.SET else HomeState.SET)
        }
    }
}