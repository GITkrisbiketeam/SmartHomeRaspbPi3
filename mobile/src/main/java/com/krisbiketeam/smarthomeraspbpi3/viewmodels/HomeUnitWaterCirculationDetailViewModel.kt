package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.WaterCirculationHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MAX_TEMPERATURE_VAL
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MAX_TEMPERATURE_VAL_LAST_UPDATE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MIN_TEMPERATURE_VAL
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MIN_TEMPERATURE_VAL_LAST_UPDATE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitLightSwitchDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The ViewModel used in [HomeUnitLightSwitchDetailFragment].
 */
@ExperimentalCoroutinesApi
class HomeUnitWaterCirculationDetailViewModel(
    application: Application,
    homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, unitName: String?
) : HomeUnitDetailViewModelBase<WaterCirculationHomeUnit<Any>>(
    application,
    homeRepository,
    roomName,
    unitName,
    HomeUnitType.HOME_WATER_CIRCULATION
) {

    val motionHwUnitNameList: StateFlow<List<Pair<String, Boolean>>> =
        isEditMode.flatMapLatest { isEdit ->
            Timber.d("init motionHwUnitNameList isEditMode: $isEdit")
            if (isEdit) {
                combine(
                    homeRepository.homeUnitListFlow(),
                    homeRepository.hwUnitListFlow()
                ) { homeUnitList, hwUnitList ->
                    hwUnitList.filter {
                        it.type == BoardConfig.IO_EXTENDER_MCP23017_INPUT
                    }.map {
                        Pair(it.name,
                            homeUnitList.find { unit -> unit is WaterCirculationHomeUnit && unit.motionHwUnitName == it.name } != null)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val motionHwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val motionValue: MutableStateFlow<String> = MutableStateFlow("")
    val motionLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val temperatureHwUnitNameList: StateFlow<List<Pair<String, Boolean>>> =
        isEditMode.flatMapLatest { isEdit ->
            Timber.d("init motionHwUnitNameList isEditMode: $isEdit")
            if (isEdit) {
                combine(
                    homeRepository.homeUnitListFlow(),
                    homeRepository.hwUnitListFlow()
                ) { homeUnitList, hwUnitList ->
                    hwUnitList.filter {
                        BoardConfig.TEMPERATURE_HW_UNIT_LIST.contains(it.type)
                    }.map {
                        Pair(it.name,
                            homeUnitList.find { unit -> unit is WaterCirculationHomeUnit && unit.temperatureHwUnitName == it.name } != null)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val temperatureHwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val temperatureValue: MutableStateFlow<String> = MutableStateFlow("")
    val temperatureLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val temperatureMinValue: MutableStateFlow<String> = MutableStateFlow("")
    val temperatureMinLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val temperatureMaxValue: MutableStateFlow<String> = MutableStateFlow("")
    val temperatureMaxLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val temperatureThreshold: MutableStateFlow<String> = MutableStateFlow("")

    val actionTimeout: MutableStateFlow<Long?> = MutableStateFlow(null)

    override fun getHomeUnitFlow(unitType: HomeUnitType, unitName: String) =
        homeRepository.waterCirculationHomeUnitFlow(unitName)

    override fun initializeAdditionalHomeUnitStates(homeUnit: WaterCirculationHomeUnit<Any>) {
        motionHwUnitName.value = homeUnit.motionHwUnitName
        motionValue.value = homeUnit.motionValue.toString()
        motionLastUpdateTime.value =
            getLastUpdateTime(getApplication(), homeUnit.motionLastUpdateTime)
        temperatureHwUnitName.value = homeUnit.temperatureHwUnitName
        temperatureValue.value = homeUnit.temperatureValue.toString()
        temperatureLastUpdateTime.value =
            getLastUpdateTime(getApplication(), homeUnit.temperatureLastUpdateTime)
        temperatureMinValue.value = homeUnit.temperatureMin.toString()
        temperatureMinLastUpdateTime.value = getLastUpdateTime(getApplication(), homeUnit.temperatureMinLastUpdateTime)
        temperatureMaxValue.value = homeUnit.temperatureMax.toString()
        temperatureMaxLastUpdateTime.value = getLastUpdateTime(getApplication(), homeUnit.temperatureMaxLastUpdateTime)
        temperatureThreshold.value = homeUnit.temperatureThreshold.toString()
        actionTimeout.value = homeUnit.actionTimeout
    }

    fun clearMinValue(): Task<Void>? {
        Timber.d("clearMinValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let{
            homeRepository.clearMinHomeUnitValue(it, HOME_MIN_TEMPERATURE_VAL, HOME_MIN_TEMPERATURE_VAL_LAST_UPDATE)
        }
    }

    fun clearMaxValue(): Task<Void>? {
        Timber.d("clearMaxValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let { homeRepository.clearMaxHomeUnitValue(it, HOME_MAX_TEMPERATURE_VAL, HOME_MAX_TEMPERATURE_VAL_LAST_UPDATE) }
    }

    fun setValueFromSwitch(isChecked: Boolean): Task<Void>? {
        Timber.d("OnCheckedChangeListener isChecked: $isChecked")
        return if (homeUnit?.value?.value != isChecked) {
            homeUnit?.value?.let { unit ->
                homeRepository.updateHomeUnitValue(
                    unit.type, unit.name,
                    isChecked,
                    System.currentTimeMillis(),
                    LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                )
            }
        } else {
            null
        }
    }

    override fun additionalNoChangesMade(homeUnit: WaterCirculationHomeUnit<Any>): Boolean {
        return homeUnit.motionHwUnitName == motionHwUnitName.value
                && homeUnit.temperatureHwUnitName == temperatureHwUnitName.value
                && homeUnit.temperatureThreshold.toString() == temperatureThreshold.value
                && homeUnit.actionTimeout == actionTimeout.value
    }

    override fun restoreAdditionalHomeUnitInitialStates(homeUnit: WaterCirculationHomeUnit<Any>) {
        motionHwUnitName.value = homeUnit.motionHwUnitName
        temperatureHwUnitName.value = homeUnit.temperatureHwUnitName
        temperatureThreshold.value = homeUnit.temperatureThreshold.toString()
        actionTimeout.value = homeUnit.actionTimeout
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    override fun actionSaveGetCustomSavePair(): Pair<Int, Int?>? {
        return when {
            hwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_hw_unit, null
            )
            temperatureHwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_temperature_hw_unit, null
            )
            else -> null
        }
    }

    override fun getHomeUnitToSave(): WaterCirculationHomeUnit<Any> {
        return WaterCirculationHomeUnit(
            name = name.value,
            type = type.value,
            room = room.value,
            hwUnitName = hwUnitName.value,
            value = homeUnit?.value?.value,
            lastUpdateTime = homeUnit?.value?.lastUpdateTime,
            temperatureHwUnitName = temperatureHwUnitName.value,
            temperatureValue = temperatureValue.value.toFloatOrNull(),
            temperatureLastUpdateTime = temperatureLastUpdateTime.value.toLongOrNull(),
            temperatureMin = temperatureMinValue.value.toFloatOrNull(),
            temperatureMinLastUpdateTime = temperatureMinLastUpdateTime.value.toLongOrNull(),
            temperatureMax = temperatureMaxValue.value.toFloatOrNull(),
            temperatureMaxLastUpdateTime = temperatureMaxLastUpdateTime.value.toLongOrNull(),
            temperatureThreshold = temperatureThreshold.value.toFloatOrNull(),
            motionHwUnitName = motionHwUnitName.value,
            motionValue = motionValue.value.toBooleanStrictOrNull(),
            motionLastUpdateTime = motionLastUpdateTime.value.toLongOrNull(),
            actionTimeout = actionTimeout.value,
            // TODO: add mising logic
            enabled = true,
            lastTriggerSource = homeUnit?.value?.lastTriggerSource,
            firebaseNotify = firebaseNotify.value,
            firebaseNotifyTrigger = firebaseNotifyTrigger.value,
            showInTaskList = showInTaskList.value,
            unitsTasks = unitTaskList.value.toMutableMap().also {
                it.remove("")
            })
    }

}
