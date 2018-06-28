package com.krisbiketeam.data.storage

data class HomeInformation(var message: String = "",
                           var button: Boolean = false,
                           var light: Boolean = false,
                           var pressure: Float = 0f,
                           var temperature: Float = 0f)
