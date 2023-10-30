package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_FIREBASE_LOGIN_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_FIREBASE_PASS_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_HOME_NAME_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_FIREBASE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_HOME_UUID
import java.util.UUID

sealed interface WriteCharacteristicData {
    val serviceUuid: UUID
    val characteristicUuid: UUID
    val data: ByteArray
}

data class WriteHomeNameData(val name: String) : WriteCharacteristicData {
    override val serviceUuid: UUID = SERVICE_HOME_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_HOME_NAME_UUID
    override val data: ByteArray = name.toByteArray()
}

data class WriteFirebaseLoginData(val login: String) : WriteCharacteristicData {
    override val serviceUuid: UUID = SERVICE_FIREBASE_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_FIREBASE_LOGIN_UUID
    override val data: ByteArray = login.toByteArray()
}

data class WriteFirebasePasswordData(val pass: String) : WriteCharacteristicData {
    override val serviceUuid: UUID = SERVICE_FIREBASE_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_FIREBASE_PASS_UUID
    override val data: ByteArray = pass.toByteArray()
}

