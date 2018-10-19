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
        // Board Pin name this hwUnit is connected to
        var pinName: String = "",
        // Connection type see {@link ConnectionType} ex. ConnectionType.I2C
        var connectionType: ConnectionType? = null,
        // Address for multiple units connected to one input ex I2c
        var softAddress: Int? = null,
        var pinInterrupt: String? = null,
        var ioPin: String? = null,
        val internalPullUp: Boolean? = null,
        // Current value this unit holds
        var value: T? = null,
        var localtime: String = Date().toString(),
        var servertime: Map<String, String>? = ServerValue.TIMESTAMP) {
    constructor(hwHwUnit: HwUnit, value: T?, localtime: String) : this(
            hwHwUnit.name,
            hwHwUnit.location,
            hwHwUnit.pinName,
            hwHwUnit.connectionType,
            hwHwUnit.softAddress,
            hwHwUnit.pinInterrupt,
            hwHwUnit.ioPin,
            hwHwUnit.internalPullUp,
            value,
            localtime)
}
