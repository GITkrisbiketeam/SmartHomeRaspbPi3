package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.ConnectionType

data class HomeUnit(
                    // HomeUnit type name ex. "BMP280" "Light"
                    var name: String,
                    // HomeUnit Connection type see {@link ConnectionType} ex. ConnectionType.I2C
                    var connectionType: ConnectionType,
                    // Location of the sensor, ex. kitchen
                    var location: String,
                    // Board Pin name this homeUnit is connected to
                    var pinName: String,
                    // HomeUnit address for multiple units connected to one input ex I2c
                    var softAddress: String?,
                    // Current value this unit holds
                    var value: Any?)
