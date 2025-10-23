package com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TriggerType

data class HomeUnitScreenUiState(
    val showProgress: Boolean,
    val unitName: String,
    val value: HomeUnitScreenValueUiState<*>?,
    val unitType: String,
    val roomName: String?,
    val hwUnit: HomeUnitScreenHwUnitUiState?,
    val additionalSettings: HomeUnitScreenAdditionalSettingsUiState?,
    val firebaseNotify: Boolean,
    @TriggerType
    val firebaseNotifyTrigger: String?,
    val showInTaskList: Boolean
)

sealed interface HomeUnitScreenValueUiState<T : Any> {
    val value: T?
    val lastUpdateTime: String

    data class NumberedValueUiState(
        override val value: Number?,
        override val lastUpdateTime: String,
        val minValue: Number?,
        val minLastUpdateTime: String,
        val maxValue: Number?,
        val maxLastUpdateTime: String,
        val clearMinValue: () -> Unit,
        val clearMaxValue: () -> Unit,
    ) : HomeUnitScreenValueUiState<Number>

    interface ActuatorValueUiState : HomeUnitScreenValueUiState<Boolean> {
        val setValueFromSwitch: (Boolean) -> Unit
    }

    data class SwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
    ) : ActuatorValueUiState

    data class LightSwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val switchValue: Boolean?,
        val switchLastUpdateTime: String,
    ) : ActuatorValueUiState

    data class WaterCirculationValueUiState(
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
    ) : ActuatorValueUiState

    data class WatchDogValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: String,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val inputValue: Boolean?,
        val inputLastUpdateTime: String,
    ) : ActuatorValueUiState
}

sealed interface HomeUnitScreenHwUnitUiState {
    val hwUnitName: String

    data class GeneralHwUnitUiState(
        override val hwUnitName: String
    ) : HomeUnitScreenHwUnitUiState

    data class LightSwitchHwUnitUiState(
        override val hwUnitName: String,
        val switchHwUnitName: String
    ) : HomeUnitScreenHwUnitUiState

    data class WatchDogHwUnitUiState(
        override val hwUnitName: String,
        val inputHwUnitName: String
    ) : HomeUnitScreenHwUnitUiState

    data class WaterCirculationHwUnitUiState(
        override val hwUnitName: String,
        val motionHwUnitName: String,
        val temperatureHwUnitName: String
    ) : HomeUnitScreenHwUnitUiState
}

sealed interface HomeUnitScreenAdditionalSettingsUiState {
    data class WaterCirculationAdditionalSettingsUiState(
        val circulationDuration: Long?,
        val temperatureThreshold: Float?
    ) : HomeUnitScreenAdditionalSettingsUiState

    data class WatchDogAdditionalSettingsUiState(
        val watchDogDelay: Long?,
        val watchDogTimeout: Long?
    ) : HomeUnitScreenAdditionalSettingsUiState
}