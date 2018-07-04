package com.krisbiketeam.data.storage.dto

data class Room(var name: String = "",
                var floor: Long = 0,
                var temperatures: List<Temperature> = ArrayList(),
                var lights: List<Light> = ArrayList(),
                var reedSwitch: List<ReedSwitch> = ArrayList(),
                var motions: List<Motion> = ArrayList(),
                var blinds: List<Blind> = ArrayList(),
                var pressures: List<Pressure> = ArrayList())