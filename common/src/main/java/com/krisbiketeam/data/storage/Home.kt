package com.krisbiketeam.data.storage

import com.krisbiketeam.data.storage.dto.*

class Home {
    var rooms: MutableMap<String, Room> = HashMap()
    var temperatures: MutableMap<String, Temperature> = HashMap()
    var lights: MutableMap<String, Light> = HashMap()
    var reedSwitches: MutableMap<String, ReedSwitch> = HashMap()
    var motions: MutableMap<String, Motion> = HashMap()
    var blinds: MutableMap<String, Blind> = HashMap()
    var pressures: MutableMap<String, Pressure> = HashMap()

    init {
        var roomName = "Kitchen"
        var temp = Temperature("Kitchen 1", roomName)
        var pressure = Pressure("Kitchen 1", roomName)
        var light = Light("Kitchen 1", roomName)
        var reedSwitch = ReedSwitch("Kitchen 1", roomName)
        val motion = Motion("Kitchen 1", roomName)
        temperatures[temp.name] = temp
        lights[light.name] = light
        reedSwitches[reedSwitch.name] = reedSwitch
        motions[motion.name] = motion
        pressures[pressure.name] = pressure
        var room = Room(roomName, 0, listOf(temp), listOf(light), listOf(reedSwitch), listOf(motion), ArrayList(), listOf(pressure))
        rooms[room.name] = room

        roomName = "Bathroom"
        temp = Temperature("Bathroom 1", roomName)
        light = Light("Bathroom 1", roomName)
        temperatures[temp.name] = temp
        lights[light.name] = light
        room = Room(roomName, 0, listOf(temp), listOf(light), listOf(reedSwitch), listOf(motion))
        rooms[room.name] = room
    }

    fun saveToRepository(homeRepository: HomeInformationRepository){
        rooms.values.forEach{homeRepository.saveRoom(it)}
        blinds.values.forEach{homeRepository.saveBlind(it)}
        lights.values.forEach{homeRepository.saveLight(it)}
        motions.values.forEach{homeRepository.saveMotion(it)}
        pressures.values.forEach{homeRepository.savePressure(it)}
        reedSwitches.values.forEach{homeRepository.saveReedSwitch(it)}
        temperatures.values.forEach{homeRepository.saveTemperature(it)}
    }
}