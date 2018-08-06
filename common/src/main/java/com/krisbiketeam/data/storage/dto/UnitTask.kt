package com.krisbiketeam.data.storage.dto

data class UnitTask(var storageUnitName: String? = null,
                    var hardwareUnitName: String? = null,
                    var delay: Long = 0,
                    var duration: Long = 0,
                    var period: Long = 0,
                    var startTime: Long = 0,
                    var endTime: Long = 0,
                    var inverse: Boolean = false)