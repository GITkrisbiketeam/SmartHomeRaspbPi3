package com.krisbiketeam.data.storage.dto

data class Motion(var name: String,
                  var room: String = "",
                  var active: Boolean = false) {
    val id: Int = hashCode()
}