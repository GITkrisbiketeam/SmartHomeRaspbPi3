package com.krisbiketeam.data.storage

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import java.util.*

@IgnoreExtraProperties
data class HomeUnit<T>(
                        // HomeUnit type name ex. "BMP280" "Light"
                        var name: String,
                        // Location of the sensor, ex. kitchen
                        var location: String,
                        // Board Pin name this homeUnit is connected to
                        var pinName: String,
                        // HomeUnit Connection type see {@link ConnectionType} ex. ConnectionType.I2C
                        var connectionType: ConnectionType,
                        // HomeUnit address for multiple units connected to one input ex I2c
                        var softAddress: Int? = null,
                        // Current value this unit holds
                        var value: T? = null,
                        var localtime: String = Date().toString(),
                        var servertime: Map<String, String>? = ServerValue.TIMESTAMP)
