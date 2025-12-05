package com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TriggerType
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartAlertDialogModel
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartListBottomSheetModel
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.Editable

data class HomeUnitScreenUiState(
    val showProgress: Boolean,
    val unitName: String,
    val value: HomeUnitScreenValueUiState<*>?,
    val unitType: Editable<String>,
    val roomName: Editable<String?>,
    val hwUnit: HomeUnitScreenHwUnitUiState,
    val additionalSettings: HomeUnitScreenAdditionalSettingsUiState?,
    val firebaseNotify: Boolean,
    @TriggerType
    val firebaseNotifyTrigger: Editable<String?>?,
    val showInTaskList: Boolean,
    val lastTriggerSource: String?,
    val unitTasks: List<String>,
    val alertDialog: SmartAlertDialogModel? = null,
    val listBottomSheet: SmartListBottomSheetModel<*,*>? = null
)

sealed interface HomeUnitScreenValueUiState<T : Any> {
    val value: T?
    val lastUpdateTime: Long?

    data class NumberedValueUiState(
        override val value: Number?,
        override val lastUpdateTime: Long?,
        val minValue: Number?,
        val minLastUpdateTime: Long?,
        val maxValue: Number?,
        val maxLastUpdateTime: Long?,
        val clearMinValue: () -> Unit,
        val clearMaxValue: () -> Unit,
    ) : HomeUnitScreenValueUiState<Number>

    interface ActuatorValueUiState : HomeUnitScreenValueUiState<Boolean> {
        val setValueFromSwitch: (Boolean) -> Unit
    }

    data class SwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: Long?,
        override val setValueFromSwitch: (Boolean) -> Unit,
    ) : ActuatorValueUiState

    data class LightSwitchValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: Long?,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val switchValue: Boolean?,
        val switchLastUpdateTime: Long?,
    ) : ActuatorValueUiState

    data class WaterCirculationValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: Long?,
        override val setValueFromSwitch: (Boolean) -> Unit,

        val motionValue: Boolean?,
        val motionLastUpdateTime: Long?,

        val temperatureValue: Number?,
        val temperatureLastUpdateTime: Long?,

        val temperatureMinValue: Number?,
        val temperatureMinLastUpdateTime: Long?,
        val temperatureMaxValue: Number?,
        val temperatureMaxLastUpdateTime: Long?,
        val clearTemperatureMinValue: () -> Unit,
        val clearTemperatureMaxValue: () -> Unit,
    ) : ActuatorValueUiState

    data class WatchDogValueUiState(
        override val value: Boolean?,
        override val lastUpdateTime: Long?,
        override val setValueFromSwitch: (Boolean) -> Unit,
        val inputValue: Boolean?,
        val inputLastUpdateTime: Long?,
    ) : ActuatorValueUiState
}

sealed interface HomeUnitScreenHwUnitUiState {
    val hwUnitName: Editable<String?>

    data class GeneralHwUnitUiState(
        override val hwUnitName: Editable<String?>
    ) : HomeUnitScreenHwUnitUiState

    data class LightSwitchHwUnitUiState(
        override val hwUnitName: Editable<String?>,
        val switchHwUnitName: Editable<String?>
    ) : HomeUnitScreenHwUnitUiState

    data class WatchDogHwUnitUiState(
        override val hwUnitName: Editable<String?>,
        val inputHwUnitName: Editable<String?>
    ) : HomeUnitScreenHwUnitUiState

    data class WaterCirculationHwUnitUiState(
        override val hwUnitName: Editable<String?>,
        val motionHwUnitName: Editable<String?>,
        val temperatureHwUnitName: Editable<String?>
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