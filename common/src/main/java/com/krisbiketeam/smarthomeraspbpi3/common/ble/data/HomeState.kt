package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

enum class HomeState(val value: Int) {

    NONE(0),
    SET(1);

    companion object {
        fun getState(state: Int): HomeState {
            return entries.find {
                it.value == state
            } ?: NONE
        }
    }
}