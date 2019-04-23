package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class Room(var name: String = "",
                var floor: Long = 0,
                var homeUnits: MutableMap<String, MutableList<String>> = mutableMapOf())