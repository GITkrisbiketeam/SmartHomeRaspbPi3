package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class Bme680Data(val timestamp: Long, val iaq: Float, val iaqAccuracy: Int, val temperature: Float, val humidity: Float,
                      val pressure: Float, val rawTemperature: Float, val rawHumidity: Float, val gas: Float,
                      val bsecStatus: Int, val staticIaq: Float, val staticIaqAccuracy: Int,
                      val co2Equivalent: Float, val co2EquivalentAccuracy: Int,
                      val breathVocEquivalent: Float, val breathVocEquivalentAccuracy: Int,
                      val compGasValue: Float, val compGasAccuracy: Int,
                      val gasPercentage: Float, val gasPercentageAccuracy: Int)