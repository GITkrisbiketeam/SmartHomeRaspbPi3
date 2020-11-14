package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class UnitTask(var name: String = "",
                    var homeUnitName: String = "",
                    var homeUnitType: String = "",
                    //var hwUnitName: String? = null,
                    var delay: Long? = null,
                    var duration: Long? = null,
                    var period: Long? = null,
                    var startTime: Long? = null,
                    var endTime: Long? = null,
                    var inverse: Boolean? = null,
                    var threshold: Float? = null,
                    var hysteresis: Float? = null)