package com.krisbiketeam.data.storage.dto

data class Room(var name: String = "",
                var floor: Long = 0,
                var lights: List<String> = ArrayList(),
                var lightSwitch: List<String> = ArrayList(),
                var reedSwitch: List<String> = ArrayList(),
                var motions: List<String> = ArrayList(),
                var temperatures: List<String> = ArrayList(),
                var pressures: List<String> = ArrayList(),
                var blinds: List<String> = ArrayList())