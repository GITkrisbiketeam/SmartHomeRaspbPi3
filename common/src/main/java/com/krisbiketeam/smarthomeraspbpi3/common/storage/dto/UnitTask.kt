package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import androidx.annotation.StringDef

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
@StringDef(BOTH, RISING_EDGE, FALLING_EDGE)
@Retention(AnnotationRetention.SOURCE)
annotation class TriggerType

const val BOTH = "both"
const val RISING_EDGE = "rising_edge"
const val FALLING_EDGE = "falling_edge"

val TRIGGER_TYPE_LIST: List<String> = listOf(BOTH, RISING_EDGE,FALLING_EDGE)

data class UnitTask(var name: String = "",
                    var homeUnitName: String = "",
                    var homeUnitType: String = "",
                    //var hwUnitName: String? = null,
                    @TriggerType var trigger: String? = null,
                    var inverse: Boolean? = null,
                    var delay: Long? = null,
                    var duration: Long? = null,
                    var period: Long? = null,
                    var startTime: Long? = null,
                    var endTime: Long? = null,
                    var threshold: Float? = null,
                    var hysteresis: Float? = null)