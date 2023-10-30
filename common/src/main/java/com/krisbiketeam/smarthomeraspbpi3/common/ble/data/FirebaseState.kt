package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

enum class FirebaseState(val value: Int) {

    NONE(0),
    SET(1),
    LOGGED(2),
    LOGGED_IN(3),
    NOT_LOGGED(4);

    companion object {
        fun getState(state: Int): FirebaseState {
            return FirebaseState.values().find {
                it.value == state
            } ?: NONE
        }
    }
}