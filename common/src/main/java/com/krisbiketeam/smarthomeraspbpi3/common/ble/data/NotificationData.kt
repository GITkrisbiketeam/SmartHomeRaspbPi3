package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_FIREBASE_STATE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_HOME_NAME_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_HOME_STATE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_FIREBASE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_HOME_UUID
import java.util.UUID

sealed interface NotificationData {
    val serviceUuid: UUID
    val characteristicUuid: UUID
    val data: ByteArray
}

data class HomeNameNotification(val name: String?) : NotificationData {
    override val serviceUuid: UUID = SERVICE_HOME_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_HOME_NAME_UUID
    override val data: ByteArray = name?.toByteArray() ?: byteArrayOf()
}

data class HomeStateNotification(val state: HomeState) : NotificationData {
    override val serviceUuid: UUID = SERVICE_HOME_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_HOME_STATE_UUID
    override val data: ByteArray = byteArrayOf(state.value.toByte())
}

data class FirebaseStateNotification(val state: FirebaseState) : NotificationData {
    override val serviceUuid: UUID = SERVICE_FIREBASE_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_FIREBASE_STATE_UUID
    override val data: ByteArray = byteArrayOf(state.value.toByte())
}

