package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import com.krisbiketeam.data.storage.ConnectionType
import java.util.*

@IgnoreExtraProperties
data class HomeUnitLog<T>(
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
        // Current value this unit holds
        var value: T? = null,
        var localtime: String = Date().toString(),
        var servertime: Map<String, String>? = ServerValue.TIMESTAMP)