package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class Bme680Data(val timestamp: Long, val iaq: Float, val iaqAccuracy: Int, val temperature: Float, val humidity: Float,
                      val pressure: Float, val rawTemperature: Float, val rawHumidity: Float, val gas: Float,
                      val bsecStatus: Int, val staticIaq: Float, val co2Equivalent: Float, val breathVocEquivalent: Float)