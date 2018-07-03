package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_BLINDS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BUTTON
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_LIGHT
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_MESSAGE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_PRESSURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_TEMPERATURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_LIGHTS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_MOTIONS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_PRESSURES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_REED_SWITCHES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_ROOMS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_TEMPERATURES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.LOG_INFORMATION_BASE
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.data.storage.livedata.*

interface HomeInformationRepository {
    fun saveMessage(message: String)
    fun saveLightState(isOn: Boolean)
    fun saveButtonState(isPressed: Boolean)
    fun saveTemperature(temperature: Float)
    fun savePressure(pressure: Float)

    fun lightLiveData(): LiveData<HomeInformation>
    fun logUnitEvent(homeUnit: HomeUnit<out Any>)

    fun saveRooms(rooms: Map<String, Room>)
    fun saveBlinds(blinds: Map<String, Blind>)
    fun saveLights(lights: Map<String, Light>)
    fun saveMotions(motions: Map<String, Motion>)
    fun savePressures(pressures: Map<String, Pressure>)
    fun saveTemperatures(temperatures: Map<String, Temperature>)
    fun saveReedSwitches(reedSwitches: Map<String, ReedSwitch>)

    fun saveRoom(room: Room)
    fun saveBlind(blind: Blind)
    fun saveLight(light: Light)
    fun saveMotion(motion: Motion)
    fun savePressure(pressure: Pressure)
    fun saveTemperature(temperature: Temperature)
    fun saveReedSwitch(reedSwitch: ReedSwitch)

    fun roomsLiveData(): LiveData<Room>
    fun blindsLiveData(): LiveData<Blind>
    fun lightsLiveData(): LiveData<Light>
    fun motionsLiveData(): LiveData<Motion>
    fun pressuresLiveData(): LiveData<Pressure>
    fun temperaturesLiveData(): LiveData<Temperature>
    fun reedSwitchesLiveData(): LiveData<ReedSwitch>
}

class FirebaseHomeInformationRepository : HomeInformationRepository {

    private val referenceHome = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)
    private val lightLiveData = HomeInformationLiveData(referenceHome)

    private val roomsLiveData = RoomsLiveData(referenceHome.child(HOME_ROOMS))
    private val blindsLiveData = BlindsLiveData(referenceHome.child(HOME_BLINDS))
    private val lightsLiveData = LightsLiveData(referenceHome.child(HOME_LIGHTS))
    private val motionsLiveData = MotionsLiveData(referenceHome.child(HOME_MOTIONS))
    private val pressuresLiveData = PressuresLiveData(referenceHome.child(HOME_PRESSURES))
    private val temperaturesLiveData = TemperaturesLiveData(referenceHome.child(HOME_TEMPERATURES))
    private val reedSwitchesLiveData = ReedSwitchesLiveData(referenceHome.child(HOME_REED_SWITCHES))

    override fun saveMessage(message: String) {
        referenceHome.child(HOME_INFORMATION_MESSAGE).setValue(message)
    }

    override fun saveLightState(isOn: Boolean) {
        referenceHome.child(HOME_INFORMATION_LIGHT).setValue(isOn)
    }

    override fun saveButtonState(isPressed: Boolean) {
        referenceHome.child(HOME_INFORMATION_BUTTON).setValue(isPressed)
    }

    override fun saveTemperature(temperature: Float) {
        referenceHome.child(HOME_INFORMATION_TEMPERATURE).setValue(temperature)
    }

    override fun savePressure(pressure: Float) {
        referenceHome.child(HOME_INFORMATION_PRESSURE).setValue(pressure)
    }

    override fun lightLiveData(): LiveData<HomeInformation> {
        return lightLiveData
    }


    override fun logUnitEvent(homeUnit: HomeUnit<out Any>) {
        referenceLog.push().setValue(homeUnit)
    }


    override fun saveRooms(rooms: Map<String, Room>) {
        referenceHome.child(HOME_ROOMS).setValue(rooms)
    }

    override fun saveBlinds(blinds: Map<String, Blind>) {
        referenceHome.child(HOME_BLINDS).setValue(blinds)
    }

    override fun saveLights(lights: Map<String, Light>) {
        referenceHome.child(HOME_LIGHTS).setValue(lights)
    }

    override fun saveMotions(motions: Map<String, Motion>) {
        referenceHome.child(HOME_MOTIONS).setValue(motions)
    }

    override fun savePressures(pressures: Map<String, Pressure>) {
        referenceHome.child(HOME_PRESSURES).setValue(pressures)
    }

    override fun saveTemperatures(temperatures: Map<String, Temperature>) {
        referenceHome.child(HOME_TEMPERATURES).setValue(temperatures)
    }

    override fun saveReedSwitches(reedSwitches: Map<String, ReedSwitch>) {
        referenceHome.child(HOME_REED_SWITCHES).setValue(reedSwitches)
    }

    override fun saveRoom(room: Room) {
        referenceHome.child(HOME_ROOMS).child(room.name).setValue(room)
    }

    override fun saveBlind(blind: Blind) {
        referenceHome.child(HOME_BLINDS).child(blind.name).setValue(blind)
    }

    override fun saveLight(light: Light) {
        referenceHome.child(HOME_LIGHTS).child(light.name).setValue(light)
    }

    override fun saveMotion(motion: Motion) {
        referenceHome.child(HOME_MOTIONS).child(motion.name).setValue(motion)
    }

    override fun savePressure(pressure: Pressure) {
        referenceHome.child(HOME_PRESSURES).child(pressure.name).setValue(pressure)
    }

    override fun saveTemperature(temperature: Temperature) {
        referenceHome.child(HOME_TEMPERATURES).child(temperature.name).setValue(temperature)
    }

    override fun saveReedSwitch(reedSwitch: ReedSwitch) {
        referenceHome.child(HOME_REED_SWITCHES).child(reedSwitch.name).setValue(reedSwitch)
    }


    override fun roomsLiveData(): LiveData<Room> {
        return roomsLiveData
    }

    override fun blindsLiveData(): LiveData<Blind> {
        return blindsLiveData
    }

    override fun lightsLiveData(): LiveData<Light> {
        return lightsLiveData
    }

    override fun motionsLiveData(): LiveData<Motion> {
        return motionsLiveData
    }

    override fun pressuresLiveData(): LiveData<Pressure> {
        return pressuresLiveData
    }

    override fun temperaturesLiveData(): LiveData<Temperature> {
        return temperaturesLiveData
    }

    override fun reedSwitchesLiveData(): LiveData<ReedSwitch> {
        return reedSwitchesLiveData
    }

}
