package com.krisbiketeam.data.storage.dto

data class ReedSwitch(var name: String,
                      var room: String = "",
                      var active: Boolean = false) {
    val id: Int = hashCode()
}