package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

enum class NetworkState(val value: Int) {

    NONE(0),
    CONNECTED(1),
    DISCONNECTED(2);

    companion object {
        fun getState(state: Int): NetworkState {
            return entries.find {
                it.value == state
            } ?: NONE
        }
    }
}