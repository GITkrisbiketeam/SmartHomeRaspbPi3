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
        var localtime: String = Date().toString(),
        var servertime: Any = ServerValue.TIMESTAMP) {

    constructor(hwHwUnit: HwUnit, value: T? = null, logMessage: String? = null, localtime: String = Date().toString()) : this(
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
            localtime)
}
