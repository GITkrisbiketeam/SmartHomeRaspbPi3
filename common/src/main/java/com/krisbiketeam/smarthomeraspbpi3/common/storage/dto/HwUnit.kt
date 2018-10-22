package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.IgnoreExtraProperties
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType

@IgnoreExtraProperties
data class HwUnit(
        // HwUnit name ex. "BMP280" "Light", Name should be unique for all units
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
        var pinInterrupt: String? = null,
        var ioPin: String? = null,
        val internalPullUp: Boolean? = null)