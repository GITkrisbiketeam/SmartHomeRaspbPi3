package com.krisbiketeam.data.storage.dto

data class UnitTask(var name: String = "",
                    var homeUnitName: String? = null,
                    var hwUnitName: String? = null,
                    var delay: Long? = null,
                    var duration: Long? = null,
                    var period: Long? = null,
                    var startTime: Long? = null,
                    var endTime: Long? = null,
                    var inverse: Boolean? = null)