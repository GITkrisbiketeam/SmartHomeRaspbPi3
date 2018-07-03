package com.krisbiketeam.data.storage.dto

data class Light(var name: String,
                 var room: String = "",
                 var on: Boolean = false) {
    val id: Int = hashCode()
}