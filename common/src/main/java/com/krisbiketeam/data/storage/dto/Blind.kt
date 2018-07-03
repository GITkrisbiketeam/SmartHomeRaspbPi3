package com.krisbiketeam.data.storage.dto

data class Blind(var name: String,
                 var room: String = "",
                 var state: Int = 0) {
    val id: Int = hashCode()
}