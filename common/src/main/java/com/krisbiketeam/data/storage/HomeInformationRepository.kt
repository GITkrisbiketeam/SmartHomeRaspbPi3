package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_BUTTON
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_LIGHT
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_MESSAGE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_PRESSURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_TEMPERATURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_BLINDS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_LIGHTS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_LIGHT_SWITCHES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_MOTIONS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_PRESSURES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_REED_SWITCHES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_ROOMS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_TEMPERATURES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.LOG_INFORMATION_BASE
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import com.krisbiketeam.data.storage.obsolete.HomeInformationLiveData

interface HomeInformationRepository {
    fun saveMessage(message: String)
    fun saveLightState(isOn: Boolean)
    fun saveButtonState(isPressed: Boolean)
    fun saveTemperature(temperature: Float)
    fun savePressure(pressure: Float)

    fun lightLiveData(): LiveData<HomeInformation>

    fun logUnitEvent(homeUnit: HomeUnitLog<out Any>)

    fun saveRoom(room: Room)
    fun saveBlind(blind: Blind)
    fun saveLight(light: Light)
    fun saveMotion(motion: Motion)
    fun savePressure(pressure: Pressure)
    fun saveTemperature(temperature: Temperature)
    fun saveLightSwitch(lightSwitch: LightSwitch)
    fun saveReedSwitch(reedSwitch: ReedSwitch)

    fun unitsLiveData(): UnitsLiveData

    fun clearLog()
}

class FirebaseHomeInformationRepository : HomeInformationRepository {

    private val referenceOldHome = FirebaseDatabase.getInstance().reference.child(OLD_HOME_INFORMATION_BASE)
    private val lightLiveData = HomeInformationLiveData(referenceOldHome)

    private val referenceHome = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)

    private val unitsDataList: UnitsLiveData = UnitsLiveData(referenceHome)

    init {
        FirebaseDatabase.getInstance().reference.keepSynced(true)
    }

    override fun saveMessage(message: String) {
        referenceOldHome.child(OLD_HOME_INFORMATION_MESSAGE).setValue(message)
    }

    override fun saveLightState(isOn: Boolean) {
        referenceOldHome.child(OLD_HOME_INFORMATION_LIGHT).setValue(isOn)
    }

    override fun saveButtonState(isPressed: Boolean) {
        referenceOldHome.child(OLD_HOME_INFORMATION_BUTTON).setValue(isPressed)
    }

    override fun saveTemperature(temperature: Float) {
        referenceOldHome.child(OLD_HOME_INFORMATION_TEMPERATURE).setValue(temperature)
    }

    override fun savePressure(pressure: Float) {
        referenceOldHome.child(OLD_HOME_INFORMATION_PRESSURE).setValue(pressure)
    }

    override fun lightLiveData(): LiveData<HomeInformation> {
        return lightLiveData
    }


    override fun logUnitEvent(homeUnit: HomeUnitLog<out Any>) {
        referenceLog.push().setValue(homeUnit)
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

    override fun saveLightSwitch(lightSwitch: LightSwitch) {
        referenceHome.child(HOME_LIGHT_SWITCHES).child(lightSwitch.name).setValue(lightSwitch)
    }

    override fun saveReedSwitch(reedSwitch: ReedSwitch) {
        referenceHome.child(HOME_REED_SWITCHES).child(reedSwitch.name).setValue(reedSwitch)
    }


    override fun unitsLiveData(): UnitsLiveData {
        return unitsDataList
    }


    override fun clearLog() {
        referenceLog.removeValue()
    }
}
