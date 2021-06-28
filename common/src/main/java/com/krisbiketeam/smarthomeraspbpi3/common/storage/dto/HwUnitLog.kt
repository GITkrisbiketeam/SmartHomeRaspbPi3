package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import java.util.*

@IgnoreExtraProperties
data class HwUnitLog<T>(
        // HwUnit type name ex. "BMP280" "Light"
        var name: String = "",
        // Location of the sensor, ex. kitchen
        var location: String = "",
        // HwUnit type
        var type: String = "",
        // Board Pin name this hwUnit is connected to
        var pinName: String = "",
        // Connection type see {@link ConnectionType} ex. ConnectionType.I2C
        var connectionType: ConnectionType? = null,
        // Address for multiple units connected to one input ex I2c
        var softAddress: Int? = null,
        // Interrupt GPIO Pin name for Alerting events
        var pinInterrupt: String? = null,
        // Pin Name for Units supporting multiple inputs/outputs like Extenders
        var ioPin: String? = null,
        // Should Software pullup be applied to Extenders Units
        val internalPullUp: Boolean? = null,
        // Refresh rate for Sensor Type Units
        val refreshRate: Long? = null,

        // Current value this unit holds
        var value: T? = null,
        var logMessage: String? = null,
        var localtime: Long = System.currentTimeMillis(),
        var localtimeString: String = Date(localtime).toString(),
        var servertime: Any = ServerValue.TIMESTAMP) {

    constructor(hwHwUnit: HwUnit, value: T? = null, logMessage: String? = null, localtime: Long = System.currentTimeMillis()) : this(
            hwHwUnit.name,
            hwHwUnit.location,
            hwHwUnit.type,
            hwHwUnit.pinName,
            hwHwUnit.connectionType,
            hwHwUnit.softAddress,
            hwHwUnit.pinInterrupt,
            hwHwUnit.ioPin,
            hwHwUnit.internalPullUp,
            hwHwUnit.refreshRate,
            value,
            logMessage,
            localtime,
            Date(localtime).toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HwUnitLog<*>

        if (name != other.name) return false
        if (location != other.location) return false
        if (type != other.type) return false
        if (pinName != other.pinName) return false
        if (connectionType != other.connectionType) return false
        if (softAddress != other.softAddress) return false
        if (pinInterrupt != other.pinInterrupt) return false
        if (ioPin != other.ioPin) return false
        if (internalPullUp != other.internalPullUp) return false
        if (refreshRate != other.refreshRate) return false
        if (value != other.value) return false
        if (logMessage != other.logMessage) return false
        if (localtime != other.localtime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + pinName.hashCode()
        result = 31 * result + (connectionType?.hashCode() ?: 0)
        result = 31 * result + (softAddress ?: 0)
        result = 31 * result + (pinInterrupt?.hashCode() ?: 0)
        result = 31 * result + (ioPin?.hashCode() ?: 0)
        result = 31 * result + (internalPullUp?.hashCode() ?: 0)
        result = 31 * result + (refreshRate?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (logMessage?.hashCode() ?: 0)
        result = 31 * result + (localtime?.hashCode() ?: 0)
        return result
    }

}

fun <T> HwUnitLog<T>.getOnlyDateLocalTime(): Long {
    val fullLocalTIme = Date(localtime)
    val strippedLocalTime = Date(0)
    strippedLocalTime.year = fullLocalTIme.year
    strippedLocalTime.month = fullLocalTIme.month
    strippedLocalTime.date = fullLocalTIme.day
    return strippedLocalTime.time
}
