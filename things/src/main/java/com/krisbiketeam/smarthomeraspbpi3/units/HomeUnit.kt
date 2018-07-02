package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.ConnectionType

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
        var value: T? = null)
