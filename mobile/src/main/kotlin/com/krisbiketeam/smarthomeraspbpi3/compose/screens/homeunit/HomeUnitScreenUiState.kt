package com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TriggerType

data class HomeUnitScreenUiState(
    val showProgress: Boolean,
    val unitName: String,
    val unitType: String,
    val roomName: String?,
    val hwUnitName: String,
    val value: HomeUnitScreenValueUiState<*>?,
    val firebaseNotify: Boolean,
    @TriggerType
    val firebaseNotifyTrigger: String?,
    val showInTaskList: Boolean
)

sealed interface HomeUnitScreenValueUiState<T : Any> {
    val value: T?
    val lastUpdateTime: String

    data class HomeUnitScreenNumberedValueUiState(
        override val value: Number?,
        override val lastUpdateTime: String,
        val minValue: Number?,
        val minLastUpdateTime: String,
        val maxValue: Number?,
        val maxLastUpdateTime: String,
        val clearMinValue: () -> Unit,
        val clearMaxValue: () -> Unit,
    ) : HomeUnitScreenValueUiState<Number>

    interface HomeUnitScreenActuatorValueUiState : HomeUnitScreenValueUiState<Boolean> {
        val setValueFromSwitch: (Boolean) -> Unit
    }

    open class HomeUnitScreenSwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
    ) : HomeUnitScreenActuatorValueUiState

    class HomeUnitScreenLightSwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val switchValue: Boolean?,
        val switchLastUpdateTime: String,
    ) : HomeUnitScreenActuatorValueUiState

    class HomeUnitScreenWatchDogValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val inputValue: Boolean?,
        val inputLastUpdateTime: String,
    ) : HomeUnitScreenActuatorValueUiState

    class HomeUnitScreenWaterCirculationValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,

        val motionValue: Boolean?,
        val motionLastUpdateTime: String,

        val temperatureValue: Number?,
        val temperatureLastUpdateTime: String,

        val temperatureMinValue: Number?,
        val temperatureMinLastUpdateTime: String,
        val temperatureMaxValue: Number?,
        val temperatureMaxLastUpdateTime: String,
        val clearTemperatureMinValue: () -> Unit,
        val clearTemperatureMaxValue: () -> Unit,
    ) : HomeUnitScreenActuatorValueUiState
}