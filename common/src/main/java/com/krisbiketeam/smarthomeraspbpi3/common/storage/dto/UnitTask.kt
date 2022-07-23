package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import androidx.annotation.StringDef
import com.google.firebase.database.Exclude
import kotlinx.coroutines.Job

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY
)
@StringDef(BOTH, RISING_EDGE, FALLING_EDGE)
@Retention(AnnotationRetention.SOURCE)
annotation class TriggerType

const val BOTH = "both"
const val RISING_EDGE = "rising_edge"
const val FALLING_EDGE = "falling_edge"

val TRIGGER_TYPE_LIST: List<String> = listOf(BOTH, RISING_EDGE, FALLING_EDGE)

data class UnitTask(var name: String = "",
                    //var homeUnitName: String = "",
                    //var homeUnitType: String = "",
                    var homeUnitsList: List<UnitTaskHomeUnit> = emptyList(),
                    //var hwUnitName: String? = null,
                    @TriggerType var trigger: String? = null,
                    var inverse: Boolean? = null,
                    var resetOnInverseTrigger: Boolean? = null,
                    var delay: Long? = null,
                    var duration: Long? = null,
                    var periodically: Boolean? = null,
                    var periodicallyOnlyHw: Boolean? = null,
                    var startTime: Long? = null,
                    var endTime: Long? = null,
                    var threshold: Float? = null,
                    var hysteresis: Float? = null,
                    var disabled: Boolean? = null){

    @Exclude
    @set:Exclude
    @get:Exclude
    var taskJob: Job? = null
}

data class UnitTaskHomeUnit(val type:String = "", val name: String = "")