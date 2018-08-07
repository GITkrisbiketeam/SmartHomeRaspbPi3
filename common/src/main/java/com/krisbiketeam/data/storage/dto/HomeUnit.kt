package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import com.krisbiketeam.data.storage.ConnectionType
import java.util.*

@IgnoreExtraProperties
data class HomeUnit(
        // HomeUnitLog type name ex. "BMP280" "Light"
        var name: String,
        // Location of the sensor, ex. kitchen
        var location: String,
        // Board Pin name this homeUnit is connected to
        var pinName: String,
        // HomeUnitLog Connection type see {@link ConnectionType} ex. ConnectionType.I2C
        var connectionType: ConnectionType,
        // HomeUnitLog address for multiple units connected to one input ex I2c
        var softAddress: Int? = null,
        var pinInterrupt: String? = null,
        var ioPin: String? = null,
        val internalPullUp: Boolean? = null)