package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.DEFAULT_WATCH_DOG_DELAY
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.DEFAULT_WATCH_DOG_TIMEOUT
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.MCP23017WatchDogHomeUnit
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
class HomeUnitMCP23017WatchDogDetailViewModel(
    application: Application,
    homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, unitName: String?
) : HomeUnitDetailViewModelBase<MCP23017WatchDogHomeUnit<Any>>(
    application,
    homeRepository,
    roomName,
    unitName,
    HomeUnitType.HOME_MCP23017_WATCH_DOG
) {

    val inputHwUnitNameList: StateFlow<List<Pair<String, Boolean>>> =
        isEditMode.flatMapLatest { isEdit ->
            Timber.d("init hwUnitNameList isEditMode: $isEdit")
            if (isEdit) {
                combine(
                    homeRepository.homeUnitListFlow(),
                    homeRepository.hwUnitListFlow()
                ) { homeUnitList, hwUnitList ->
                    hwUnitList.filter {
                        it.type == BoardConfig.IO_EXTENDER_MCP23017_INPUT
                    }.map {
                        Pair(it.name,
                            homeUnitList.find { unit -> unit.hwUnitName == it.name || (unit is MCP23017WatchDogHomeUnit<*> && unit.inputHwUnitName == it.name) } != null)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val inputHwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val inputValue: MutableStateFlow<String> = MutableStateFlow("")
    val inputLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val watchDogDelay: MutableStateFlow<Long?> = MutableStateFlow(null)
    val watchDogTimeout: MutableStateFlow<Long?> = MutableStateFlow(null)

    override fun getHomeUnitFlow(unitType: HomeUnitType, unitName: String) =
        homeRepository.mcp23017WatchDogHomeUnitFlow(unitName)

    override fun initializeAdditionalHomeUnitStates(homeUnit: MCP23017WatchDogHomeUnit<Any>) {
        inputHwUnitName.value = homeUnit.inputHwUnitName
        inputValue.value = homeUnit.inputValue.toString()
        inputLastUpdateTime.value =
            getLastUpdateTime(getApplication(), homeUnit.inputLastUpdateTime)
        watchDogDelay.value = homeUnit.watchDogDelay
        watchDogTimeout.value = homeUnit.watchDogTimeout
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

    override fun additionalNoChangesMade(homeUnit: MCP23017WatchDogHomeUnit<Any>): Boolean {
        return homeUnit.inputHwUnitName == inputHwUnitName.value
                && homeUnit.watchDogDelay == watchDogDelay.value
                && homeUnit.watchDogTimeout == watchDogTimeout.value
    }

    override fun restoreAdditionalHomeUnitInitialStates(homeUnit: MCP23017WatchDogHomeUnit<Any>) {
        inputHwUnitName.value = homeUnit.inputHwUnitName
        watchDogDelay.value = homeUnit.watchDogDelay
        watchDogTimeout.value = homeUnit.watchDogTimeout
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    override fun actionSaveGetCustomSavePair(): Pair<Int, Int?>? {
        return when {
            hwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_hw_unit, null
            )
            inputHwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_second_hw_unit, null
            )
            else -> null
        }
    }

    override fun getHomeUnitToSave(): MCP23017WatchDogHomeUnit<Any> {
        return MCP23017WatchDogHomeUnit(
            name = name.value,
            type = type.value,
            room = room.value,
            hwUnitName = hwUnitName.value,
            value = homeUnit?.value?.value,
            lastUpdateTime = homeUnit?.value?.lastUpdateTime,
            inputHwUnitName = inputHwUnitName.value,
            inputValue = inputValue.value.toBooleanStrictOrNull(),
            //inputLastUpdateTime = inputLastUpdateTime.value,
            watchDogDelay = watchDogDelay.value ?: DEFAULT_WATCH_DOG_DELAY,
            watchDogTimeout = watchDogTimeout.value ?: DEFAULT_WATCH_DOG_TIMEOUT,
            lastTriggerSource = homeUnit?.value?.lastTriggerSource,
            firebaseNotify = firebaseNotify.value,
            firebaseNotifyTrigger = firebaseNotifyTrigger.value,
            showInTaskList = showInTaskList.value,
            unitsTasks = unitTaskList.value.toMutableMap().also {
                it.remove("")
            })
    }

}
