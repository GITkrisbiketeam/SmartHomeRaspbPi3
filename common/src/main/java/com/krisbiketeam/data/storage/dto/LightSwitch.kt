package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.Exclude

data class LightSwitch(var name: String = "",
                       var room: String = "",
                       var unitName: String = "",
                       var lightName: String = "",
                       var active: Boolean = false) {

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: LightSwitch.(Boolean) -> Unit = { Unit }
}