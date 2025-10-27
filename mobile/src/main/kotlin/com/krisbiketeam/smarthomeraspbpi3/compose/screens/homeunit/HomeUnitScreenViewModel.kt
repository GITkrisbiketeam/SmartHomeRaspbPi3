package com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.DEFAULT_WATCH_DOG_DELAY
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.DEFAULT_WATCH_DOG_TIMEOUT
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_ACTION_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.MCP23017WatchDogHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.WaterCirculationHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MAX_TEMPERATURE_VAL
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MAX_TEMPERATURE_VAL_LAST_UPDATE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MIN_TEMPERATURE_VAL
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_MIN_TEMPERATURE_VAL_LAST_UPDATE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.toHomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartAlertDialogModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The ViewModel used in [HomeUnitScreen].
 */
@ExperimentalCoroutinesApi
class HomeUnitScreenViewModel(
    application: Application,
    private val homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, private val unitName: String, private val unitType: HomeUnitType
) : AndroidViewModel(application) {

    private var homeUnitUiStateBeforeEditing: HomeUnitScreenUiState? = null

    private val _isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _uiState: MutableStateFlow<HomeUnitScreenUiState> = MutableStateFlow(
        HomeUnitScreenUiState(
            showProgress = true,
            unitName = unitName,
            value = null,
            unitType = unitType.firebaseTableName,
            roomName = roomName,
            hwUnit = null,
            additionalSettings = null,
            firebaseNotify = false,
            firebaseNotifyTrigger = null,
            showInTaskList = false,
            lastTriggerSource = null,
            unitTasks = emptyList()
        )
    )

    private val _navigateUp: Channel<Unit> = Channel()

    private val _snackBarText: Channel<Int> = Channel()

    val isEditMode: StateFlow<Boolean> = _isEditMode.flatMapLatest { isEditing ->
        if (!isEditing) {
            when (unitType) {
                HomeUnitType.HOME_LIGHT_SWITCHES -> {
                    homeRepository.lightSwitchHomeUnitFlow(unitName)
                }

                HomeUnitType.HOME_WATER_CIRCULATION -> {
                    homeRepository.waterCirculationHomeUnitFlow(
                        unitName
                    )
                }

                HomeUnitType.HOME_MCP23017_WATCH_DOG -> {
                    homeRepository.mcp23017WatchDogHomeUnitFlow(
                        unitName
                    )
                }

                else -> {
                    homeRepository.genericHomeUnitFlow(unitType, unitName)
                }
            }.map { homeUnit ->
                val uiState = when (homeUnit) {
                    is LightSwitchHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getLightSwitchHomeUnitValue(homeUnit as LightSwitchHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                switchHwUnitName = homeUnit.switchHwUnitName.toString()
                            ),
                            additionalSettings = null,
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            lastTriggerSource = homeUnit.lastTriggerSource,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is WaterCirculationHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getWaterCirculationHomeUnitValue(homeUnit as WaterCirculationHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                motionHwUnitName = homeUnit.motionHwUnitName.toString(),
                                temperatureHwUnitName = homeUnit.temperatureHwUnitName.toString()
                            ),
                            additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState(
                                circulationDuration = homeUnit.actionTimeout,
                                temperatureThreshold = homeUnit.temperatureThreshold
                            ),
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            lastTriggerSource = homeUnit.lastTriggerSource,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is MCP23017WatchDogHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getWatchDogHomeUnitValue(homeUnit as MCP23017WatchDogHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                switchHwUnitName = homeUnit.inputHwUnitName.toString()
                            ),
                            additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState(
                                watchDogDelay = homeUnit.watchDogDelay,
                                watchDogTimeout = homeUnit.watchDogTimeout
                            ),
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            lastTriggerSource = homeUnit.lastTriggerSource,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is GenericHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getGenericHomeUnitValue(homeUnit as GenericHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState(
                                homeUnit.hwUnitName.toString()
                            ),
                            additionalSettings = null,
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            lastTriggerSource = homeUnit.lastTriggerSource,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }
                }
                homeUnitUiStateBeforeEditing = uiState
                _uiState.value = uiState
                false
            }
        } else {
            flowOf(true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), _isEditMode.value)

    val uiState: StateFlow<HomeUnitScreenUiState> = _uiState

    val navigateUp: Flow<Unit> = _navigateUp.receiveAsFlow()

    val snackBarText: Flow<Int> = _snackBarText.receiveAsFlow()

    init {
        viewModelScope.launch {
            /*isEditMode.filter { !it }.flatMapMerge {
                when (unitType) {
                    HomeUnitType.HOME_LIGHT_SWITCHES -> {
                        homeRepository.lightSwitchHomeUnitFlow(unitName)
                    }

                    HomeUnitType.HOME_WATER_CIRCULATION -> {
                        homeRepository.waterCirculationHomeUnitFlow(
                            unitName
                        )
                    }

                    HomeUnitType.HOME_MCP23017_WATCH_DOG -> {
                        homeRepository.mcp23017WatchDogHomeUnitFlow(
                            unitName
                        )
                    }

                    else -> {
                        homeRepository.genericHomeUnitFlow(unitType, unitName)
                    }
                }
            }.collect { homeUnit ->
                _uiState.value = when (homeUnit) {
                    is LightSwitchHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getLightSwitchHomeUnitValue(homeUnit as LightSwitchHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                switchHwUnitName = homeUnit.switchHwUnitName.toString()
                            ),
                            additionalSettings = null,
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is WaterCirculationHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getWaterCirculationHomeUnitValue(homeUnit as WaterCirculationHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                motionHwUnitName = homeUnit.motionHwUnitName.toString(),
                                temperatureHwUnitName = homeUnit.temperatureHwUnitName.toString()
                            ),
                            additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState(
                                circulationDuration = homeUnit.actionTimeout,
                                temperatureThreshold = homeUnit.temperatureThreshold
                            ),
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is MCP23017WatchDogHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getWatchDogHomeUnitValue(homeUnit as MCP23017WatchDogHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState(
                                hwUnitName = homeUnit.hwUnitName.toString(),
                                switchHwUnitName = homeUnit.inputHwUnitName.toString()
                            ),
                            additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState(
                                watchDogDelay = homeUnit.watchDogDelay,
                                watchDogTimeout = homeUnit.watchDogTimeout
                            ),
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }

                    is GenericHomeUnit<*> -> {
                        HomeUnitScreenUiState(
                            showProgress = false,
                            unitName = homeUnit.name,
                            value = getGenericHomeUnitValue(homeUnit as GenericHomeUnit<Any>),
                            unitType = homeUnit.type.firebaseTableName,
                            roomName = homeUnit.room,
                            hwUnit = HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState(
                                homeUnit.hwUnitName.toString()
                            ),
                            additionalSettings = null,
                            firebaseNotify = homeUnit.firebaseNotify,
                            firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
                            showInTaskList = homeUnit.showInTaskList,
                            unitTasks = homeUnit.unitsTasks.keys.toList()
                        )
                    }
                }
            }*/
        }
    }

    private fun getGenericHomeUnitValue(homeUnit: GenericHomeUnit<Any>): HomeUnitScreenValueUiState<*>? {
        return if (HOME_ACTION_STORAGE_UNITS.contains(homeUnit.type) && homeUnit.value is Boolean?) {
            return HomeUnitScreenValueUiState.SwitchValueUiState(
                value = homeUnit.value as? Boolean,
                lastUpdateTime = homeUnit.lastUpdateTime,
                { isChecked ->
                    Timber.d("OnCheckedChangeListener isChecked: $isChecked")
                    if (homeUnit.value != isChecked) {
                        homeRepository.updateHomeUnitValue(
                            homeUnit.type, homeUnit.name,
                            isChecked,
                            System.currentTimeMillis(),
                            LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                        )
                    }
                }
            )
        } else if (homeUnit.value is Number?) {
            HomeUnitScreenValueUiState.NumberedValueUiState(
                value = homeUnit.value as? Number,
                lastUpdateTime = homeUnit.lastUpdateTime,
                minValue = homeUnit.min as? Number,
                minLastUpdateTime = homeUnit.minLastUpdateTime,
                maxValue = homeUnit.max as? Number,
                maxLastUpdateTime = homeUnit.maxLastUpdateTime,
                clearMinValue = {
                    Timber.d("clearMinValue homeUnit: $homeUnit")
                    homeRepository.clearMinHomeUnitValue(homeUnit)
                },
                clearMaxValue = {
                    Timber.d("clearMaxValue homeUnit: $homeUnit")
                    homeRepository.clearMaxHomeUnitValue(homeUnit)
                }
            )
        } else {
            null
        }
    }

    private fun getLightSwitchHomeUnitValue(homeUnit: LightSwitchHomeUnit<Any>): HomeUnitScreenValueUiState<*>? {
        return HomeUnitScreenValueUiState.LightSwitchValueUiState(
            value = homeUnit.value as? Boolean,
            lastUpdateTime = homeUnit.lastUpdateTime,
            { isChecked ->
                Timber.d("OnCheckedChangeListener isChecked: $isChecked")
                if (homeUnit.value != isChecked) {
                    homeRepository.updateHomeUnitValue(
                        homeUnit.type, homeUnit.name,
                        isChecked,
                        System.currentTimeMillis(),
                        LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                    )
                }
            },
            switchValue = homeUnit.switchValue as? Boolean,
            switchLastUpdateTime = homeUnit.switchLastUpdateTime
        )
    }

    private fun getWatchDogHomeUnitValue(homeUnit: MCP23017WatchDogHomeUnit<Any>): HomeUnitScreenValueUiState<*>? {
        return HomeUnitScreenValueUiState.WatchDogValueUiState(
            value = homeUnit.value as? Boolean,
            lastUpdateTime = homeUnit.lastUpdateTime,
            { isChecked ->
                Timber.d("OnCheckedChangeListener isChecked: $isChecked")
                if (homeUnit.value != isChecked) {
                    homeRepository.updateHomeUnitValue(
                        homeUnit.type, homeUnit.name,
                        isChecked,
                        System.currentTimeMillis(),
                        LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                    )
                }
            },
            inputValue = homeUnit.inputValue as? Boolean,
            inputLastUpdateTime = homeUnit.inputLastUpdateTime
        )
    }

    private fun getWaterCirculationHomeUnitValue(homeUnit: WaterCirculationHomeUnit<Any>): HomeUnitScreenValueUiState<*>? {
        return HomeUnitScreenValueUiState.WaterCirculationValueUiState(
            value = homeUnit.value as? Boolean,
            lastUpdateTime = homeUnit.lastUpdateTime,
            { isChecked ->
                Timber.d("OnCheckedChangeListener isChecked: $isChecked")
                if (homeUnit.value != isChecked) {
                    homeRepository.updateHomeUnitValue(
                        homeUnit.type, homeUnit.name,
                        isChecked,
                        System.currentTimeMillis(),
                        LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                    )
                }
            },
            motionValue = homeUnit.motionValue,
            motionLastUpdateTime = homeUnit.motionLastUpdateTime,
            temperatureValue = homeUnit.temperatureValue,
            temperatureLastUpdateTime = homeUnit.temperatureLastUpdateTime,

            temperatureMinValue = homeUnit.temperatureMin,
            temperatureMinLastUpdateTime = homeUnit.temperatureMinLastUpdateTime,
            temperatureMaxValue = homeUnit.temperatureMax,
            temperatureMaxLastUpdateTime = homeUnit.temperatureMaxLastUpdateTime,
            clearTemperatureMinValue = {
                Timber.d("clearMinValue homeUnit: $homeUnit")
                homeRepository.clearMinHomeUnitValue(
                    homeUnit,
                    HOME_MIN_TEMPERATURE_VAL,
                    HOME_MIN_TEMPERATURE_VAL_LAST_UPDATE
                )
            },
            clearTemperatureMaxValue = {
                Timber.d("clearMaxValue homeUnit: $homeUnit")
                homeRepository.clearMaxHomeUnitValue(
                    homeUnit,
                    HOME_MAX_TEMPERATURE_VAL,
                    HOME_MAX_TEMPERATURE_VAL_LAST_UPDATE
                )
            }
        )
    }

    //val unitTaskListAdapter = UnitTaskListAdapter(homeRepository, unitName, unitType)

    /*val homeUnit: StateFlow<T?>? =
        if (unitName.isNullOrEmpty() || unitType == HomeUnitType.UNKNOWN) null else getHomeUnitFlow(
            unitType, unitName
        ).onEach { homeUnit ->
            Timber.e("homeUnit changed:$homeUnit")
            showProgress.value = false
            name.value = homeUnit.name
            type.value = homeUnit.type
            room.value = homeUnit.room
            hwUnitName.value = homeUnit.hwUnitName
            value.value = homeUnit.value.toString()
            lastUpdateTime.value = homeUnit.lastUpdateTime
            firebaseNotify.value = homeUnit.firebaseNotify
            firebaseNotifyTrigger.value = homeUnit.firebaseNotifyTrigger ?: RISING_EDGE
            showInTaskList.value = homeUnit.showInTaskList

            initializeAdditionalHomeUnitStates(homeUnit)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

   // TODO check if possible nullable
    val name: MutableStateFlow<String> = MutableStateFlow(unitName ?: "")

    open val typeList =
        HOME_STORAGE_UNITS.filterNot { it == HomeUnitType.HOME_LIGHT_SWITCHES || it == HomeUnitType.HOME_WATER_CIRCULATION || it == HomeUnitType.HOME_MCP23017_WATCH_DOG }
    val type: MutableStateFlow<HomeUnitType> = MutableStateFlow(unitType)
    val isTypeVisible: StateFlow<Boolean> =
        MutableStateFlow(unitType != HomeUnitType.HOME_LIGHT_SWITCHES && unitType != HomeUnitType.HOME_WATER_CIRCULATION && unitType != HomeUnitType.HOME_MCP23017_WATCH_DOG)

    val roomList: StateFlow<List<String>> =
        isEditMode.flatMapLatest { isEdit ->
            if (isEdit) {
                homeRepository.roomListFlow().map { list ->
                    list.map(Room::name)
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // TODO check if possible nullable
    val room: MutableStateFlow<String> = MutableStateFlow(roomName ?: "")

    val hwUnitNameList: StateFlow<List<Pair<String, Boolean>>> =
        isEditMode.flatMapLatest { isEdit ->
            Timber.d("init hwUnitNameList isEditMode: $isEdit")
            if (isEdit) {
                combine(
                    homeRepository.homeUnitListFlow(),
                    homeRepository.hwUnitListFlow(),
                    type
                ) { homeUnitList, hwUnitList, type ->
                    homeUnitOfSelectedTypeList = homeUnitList.filter {
                        it.type == type
                    }
                    hwUnitList.filter {
                        when (type) {
                            HomeUnitType.HOME_ACTUATORS,
                            HomeUnitType.HOME_LIGHT_SWITCHES,
                            HomeUnitType.HOME_WATER_CIRCULATION,
                            HomeUnitType.HOME_MCP23017_WATCH_DOG,
                            HomeUnitType.HOME_BLINDS ->
                                it.type == BoardConfig.IO_EXTENDER_MCP23017_OUTPUT
                            HomeUnitType.HOME_MOTIONS, HomeUnitType.HOME_REED_SWITCHES ->
                                it.type == BoardConfig.IO_EXTENDER_MCP23017_INPUT
                            HomeUnitType.HOME_TEMPERATURES -> {
                                BoardConfig.TEMPERATURE_HW_UNIT_LIST.contains(it.type)
                            }
                            HomeUnitType.HOME_HUMIDITY -> {
                                BoardConfig.HUMIDITY_HW_UNIT_LIST.contains(it.type)
                            }
                            HomeUnitType.HOME_PRESSURES -> {
                                BoardConfig.PRESSURE_HW_UNIT_LIST.contains(it.type)
                            }
                            HomeUnitType.HOME_CO2,
                            HomeUnitType.HOME_GAS,
                            HomeUnitType.HOME_GAS_PERCENT,
                            HomeUnitType.HOME_IAQ,
                            HomeUnitType.HOME_STATIC_IAQ,
                            HomeUnitType.HOME_BREATH_VOC -> it.type == BoardConfig.AIR_QUALITY_SENSOR_BME680
                            HomeUnitType.UNKNOWN -> true
                        }
                    }.map {
                        Pair(it.name,
                            homeUnitList.find { unit -> unit.hwUnitName == it.name || (unit is LightSwitchHomeUnit<*> && unit.switchHwUnitName == it.name) } != null)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val hwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val value: MutableStateFlow<String> = MutableStateFlow("")
    val lastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val firebaseNotify: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val firebaseNotifyTriggerTypeList = TRIGGER_TYPE_LIST
    val firebaseNotifyTrigger: MutableStateFlow<String?> = MutableStateFlow(RISING_EDGE)
    val showFirebaseNotifyTrigger: StateFlow<Boolean> =
        combine(firebaseNotify, type) { notify, type ->
            notify && HOME_FIREBASE_NOTIFY_STORAGE_UNITS.contains(type)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val showInTaskList: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showInTaskListVisibility: StateFlow<Boolean> =
        combine(hwUnitName, type) { hwUnitName, type ->
            !hwUnitName.isNullOrEmpty() && HOME_ACTION_STORAGE_UNITS.contains(type)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Decide how to handle this list
    val unitTaskList: StateFlow<Map<String, UnitTask>> =
        if (homeUnit != null && unitType != HomeUnitType.UNKNOWN && !unitName.isNullOrEmpty()) {
            combine(
                isEditMode,
                homeRepository.unitTaskListFlow(unitType, unitName)
            ) { edit, taskList ->
                Timber.d("init unitTaskList isEditMode edit: $edit")
                if (edit) {
                    Timber.d("init unitTaskList edit: $taskList")
                    // Add empty UnitTask which will be used for adding new UnitTask
                    val newList = taskList.toMutableMap()
                    newList[""] = UnitTask()
                    Timber.d("init unitTaskList edit newList: $newList")

                    newList
                } else {
                    Timber.d("init unitTaskList: $taskList")
                    taskList
                }
            }.flowOn(Dispatchers.IO)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
        } else {
            MutableStateFlow(emptyMap())
        }
*/
    private var homeRepositoryTask: Task<Void>? = null

    // used for checking if given homeUnit name is not already used this is populated in hwUnitNameList
    private var homeUnitOfSelectedTypeList: List<HomeUnit<Any>> = emptyList()

    fun startEditing() {
        _isEditMode.value = true
    }

    fun updateUiState(editedUiState: HomeUnitScreenUiState) {
        _uiState.value = editedUiState
    }

    fun closeAlertDialog() {
        _uiState.update { it.copy(alertDialog = null) }
    }

    fun setFirebaseNotify(firebaseNotify: Boolean) {
        _uiState.update { it.copy(firebaseNotify = firebaseNotify) }
    }

    fun setShowInTaskList(showInTaskList: Boolean) {
        _uiState.update { it.copy(showInTaskList = showInTaskList) }
    }

    fun actionDiscard() {
        Timber.d(
            "actionDiscard homeUnit: $unitName of $unitType"
        )
        if (noChangesMade()) {
            _isEditMode.value = false
        } else {
            _uiState.update {
                it.copy(
                    alertDialog = SmartAlertDialogModel(
                        title = R.string.menu_discard,
                        description = R.string.add_edit_home_unit_discard_changes,
                        positiveButtonTextId = R.string.menu_discard,
                        positiveButtonAction = ::doDiscardChanges
                    )
                )
            }
        }
    }

    private fun doDiscardChanges() {
        Timber.d(
            "doDiscardChanges homeUnit: $unitName of $unitType"
        )
        _isEditMode.value = false
        homeUnitUiStateBeforeEditing?.let {
            _uiState.value = it
        }
    }

    fun actionDeleteHomeUnit() {
        Timber.d(
            "actionDeleteHomeUnit homeUnit: $unitName of $unitType"
        )
        _uiState.update {
            it.copy(
                alertDialog = SmartAlertDialogModel(
                    title = R.string.add_edit_home_unit_delete_title,
                    description = R.string.add_edit_home_unit_delete_home_unit_prompt,
                    positiveButtonTextId = R.string.menu_delete,
                    positiveButtonAction = ::doDeleteHomeUnit
                )
            )
        }
    }

    private fun doDeleteHomeUnit() {
        Timber.d(
            "doDeleteHomeUnit homeUnit: $unitName of $unitType homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}"
        )
        viewModelScope.launch(Dispatchers.IO) {
            homeRepositoryTask =
                homeRepository.deleteHomeUnit(homeUnitType = unitType, homeUnitName = unitName)
                    ?.also {
                        _uiState.update {
                            it.copy(
                                showProgress = true,
                                alertDialog = null
                            )
                        }
                    }?.addOnCompleteListener {
                        Timber.d("doDeleteHomeUnit Task completed")
                        _uiState.update { it.copy(showProgress = false) }
                        _isEditMode.value = false
                        _navigateUp.trySend(Unit)
                    }
        }
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave() {
        Timber.d("actionSave name: $unitName")
        /*if (homeUnit == null) {
            // Adding new HomeUnit
            when {
                name.value.trim().isEmpty() -> return Pair(
                    R.string.add_edit_home_unit_empty_name, null
                )
                type.value == HomeUnitType.UNKNOWN -> return Pair(
                    R.string.add_edit_home_unit_empty_unit_type, null
                )
                homeUnitOfSelectedTypeList.find { unit -> unit.name == name.value.trim() } != null -> {
                    //This name is already used
                    Timber.d("This name is already used")
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                }
            }
            actionSaveGetCustomSavePair()?.let { return it }
        } else {*/
        // Editing existing HomeUnit
        if (_uiState.value.unitName.trim().isEmpty()) {
            _snackBarText.trySend(R.string.add_edit_home_unit_empty_name)
        } else if (_uiState.value.unitName.trim().contains(Regex("[.#$\\[\\]]"))) {
            _snackBarText.trySend(R.string.add_edit_home_unit_name_illegal_character)
        } else if (_uiState.value.unitName.trim() != homeUnitUiStateBeforeEditing?.unitName
            && homeUnitOfSelectedTypeList.find { it.name == _uiState.value.unitName.trim() } != null
        ) {
            _snackBarText.trySend(R.string.add_edit_home_unit_name_already_used)
        } else if (_uiState.value.unitName.trim() != homeUnitUiStateBeforeEditing?.unitName
            || _uiState.value.unitType != homeUnitUiStateBeforeEditing?.unitType
        ) {
            _uiState.update {
                it.copy(
                    alertDialog = SmartAlertDialogModel(
                        title = R.string.overwrite,
                        description = R.string.add_edit_home_unit_save_with_delete,
                        positiveButtonTextId = R.string.overwrite,
                        positiveButtonAction = ::saveChanges
                    )
                )
            }
        } else if (noChangesMade()) {
            _snackBarText.trySend(R.string.add_edit_home_unit_no_changes)
        } else {
            _uiState.update {
                it.copy(
                    alertDialog = SmartAlertDialogModel(
                        title = R.string.overwrite,
                        description = R.string.add_edit_home_unit_overwrite_changes,
                        positiveButtonTextId = R.string.overwrite,
                        positiveButtonAction = ::saveChanges
                    )
                )
            }
        }
        // new Home Unit adding just show Save Dialog
        //return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }

    private fun saveChanges() {
        Timber.d(
            "saveChanges homeUnit: $unitName homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}"
        )
        homeRepositoryTask = if (unitName != null && unitType != null) {
            _uiState.update { it.copy(showProgress = true) }
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (_uiState.value.unitName != unitName || _uiState.value.unitType.toHomeUnitType() != unitType) {
                    Timber.d(
                        "Name or type changed will need to delete old value name=$unitName, type = $unitType"
                    )
                    // delete old HomeUnit
                    this?.continueWithTask {
                        homeRepository.deleteHomeUnit(unitType, unitName) ?: it
                    }
                }
            }
        } else {
            doSaveChanges()
        }?.addOnCompleteListener {
            Timber.d("Task completed")
            _uiState.update { it.copy(showProgress = false) }
            _isEditMode.value = false
        }
    }

    private fun doSaveChanges(): Task<Void>? {
        _uiState.update { it.copy(showProgress = false) }
        return homeRepository.saveHomeUnit(
            getHomeUnitToSave()
        )
    }

    private fun noChangesMade(): Boolean {
        return _uiState.value == homeUnitUiStateBeforeEditing
    }

    private fun getHomeUnitToSave(): HomeUnit<Any> {
        val currentUiState = _uiState.value
        val currentValueUiState = _uiState.value.value
        val currentHwUiState = _uiState.value.hwUnit
        val currentAdditionalSettings = _uiState.value.additionalSettings
        return when (currentUiState.unitType) {
            HomeUnitType.HOME_LIGHT_SWITCHES.firebaseTableName -> {
                LightSwitchHomeUnit(
                    name = currentUiState.unitName,
                    type = currentUiState.unitType.toHomeUnitType(),
                    room = currentUiState.roomName ?: "",
                    hwUnitName = currentUiState.hwUnit?.hwUnitName,
                    value = currentValueUiState?.value,
                    lastUpdateTime = currentValueUiState?.lastUpdateTime,
                    switchHwUnitName = currentHwUiState?.let {
                        if (it is HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState) {
                            it.switchHwUnitName
                        } else {
                            null
                        }
                    },
                    switchValue = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.LightSwitchValueUiState) {
                            it.switchValue
                        } else {
                            null
                        }
                    },
                    switchLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.LightSwitchValueUiState) {
                            it.switchLastUpdateTime
                        } else {
                            null
                        }
                    },
                    lastTriggerSource = currentUiState.lastTriggerSource,
                    firebaseNotify = currentUiState.firebaseNotify,
                    firebaseNotifyTrigger = currentUiState.firebaseNotifyTrigger,
                    showInTaskList = currentUiState.showInTaskList,
                    /*unitsTasks = unitTaskList.value.toMutableMap().also {
                        it.remove("")
                    }*/
                )
            }

            HomeUnitType.HOME_WATER_CIRCULATION.firebaseTableName -> {
                WaterCirculationHomeUnit(
                    name = currentUiState.unitName,
                    type = currentUiState.unitType.toHomeUnitType(),
                    room = currentUiState.roomName ?: "",
                    hwUnitName = currentUiState.hwUnit?.hwUnitName,
                    value = currentValueUiState?.value,
                    lastUpdateTime = currentValueUiState?.lastUpdateTime,
                    temperatureHwUnitName = currentHwUiState?.let {
                        if (it is HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState) {
                            it.temperatureHwUnitName
                        } else {
                            null
                        }
                    },
                    temperatureValue = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureValue?.toFloat()
                        } else {
                            null
                        }
                    },
                    temperatureLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureLastUpdateTime
                        } else {
                            null
                        }
                    },
                    temperatureMin = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureMinValue?.toFloat()
                        } else {
                            null
                        }
                    },
                    temperatureMinLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureMinLastUpdateTime
                        } else {
                            null
                        }
                    },
                    temperatureMax = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureMaxValue?.toFloat()
                        } else {
                            null
                        }
                    },
                    temperatureMaxLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.temperatureMaxLastUpdateTime
                        } else {
                            null
                        }
                    },
                    temperatureThreshold = currentAdditionalSettings?.let {
                        if (it is HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState) {
                            it.temperatureThreshold
                        } else {
                            null
                        }
                    },
                    motionHwUnitName = currentHwUiState?.let {
                        if (it is HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState) {
                            it.motionHwUnitName
                        } else {
                            null
                        }
                    },
                    motionValue = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.motionValue
                        } else {
                            null
                        }
                    },
                    motionLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WaterCirculationValueUiState) {
                            it.motionLastUpdateTime
                        } else {
                            null
                        }
                    },
                    actionTimeout = currentAdditionalSettings?.let {
                        if (it is HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState) {
                            it.circulationDuration
                        } else {
                            null
                        }
                    },
                    lastTriggerSource = currentUiState.lastTriggerSource,
                    firebaseNotify = currentUiState.firebaseNotify,
                    firebaseNotifyTrigger = currentUiState.firebaseNotifyTrigger,
                    showInTaskList = currentUiState.showInTaskList,
                    /*unitsTasks = unitTaskList.value.toMutableMap().also {
                        it.remove("")
                    }*/
                )
            }

            HomeUnitType.HOME_MCP23017_WATCH_DOG.firebaseTableName -> {
                MCP23017WatchDogHomeUnit(
                    name = currentUiState.unitName,
                    type = currentUiState.unitType.toHomeUnitType(),
                    room = currentUiState.roomName ?: "",
                    hwUnitName = currentUiState.hwUnit?.hwUnitName,
                    value = currentValueUiState?.value,
                    lastUpdateTime = currentValueUiState?.lastUpdateTime,
                    inputHwUnitName = currentHwUiState?.let {
                        if (it is HomeUnitScreenHwUnitUiState.WatchDogHwUnitUiState) {
                            it.inputHwUnitName
                        } else {
                            null
                        }
                    },
                    inputValue = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WatchDogValueUiState) {
                            it.inputValue
                        } else {
                            null
                        }
                    },
                    inputLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.WatchDogValueUiState) {
                            it.inputLastUpdateTime
                        } else {
                            null
                        }
                    },
                    watchDogTimeout = currentAdditionalSettings?.let {
                        if (it is HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState) {
                            it.watchDogTimeout
                        } else {
                            null
                        }
                    } ?: DEFAULT_WATCH_DOG_TIMEOUT,
                    watchDogDelay = currentAdditionalSettings?.let {
                        if (it is HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState) {
                            it.watchDogDelay
                        } else {
                            null
                        }
                    } ?: DEFAULT_WATCH_DOG_DELAY,
                    lastTriggerSource = currentUiState.lastTriggerSource,
                    firebaseNotify = currentUiState.firebaseNotify,
                    firebaseNotifyTrigger = currentUiState.firebaseNotifyTrigger,
                    showInTaskList = currentUiState.showInTaskList,
                    /*unitsTasks = unitTaskList.value.toMutableMap().also {
                        it.remove("")
                    }*/
                )
            }

            else -> {
                GenericHomeUnit(
                    name = currentUiState.unitName,
                    type = currentUiState.unitType.toHomeUnitType(),
                    room = currentUiState.roomName ?: "",
                    hwUnitName = currentUiState.hwUnit?.hwUnitName,
                    value = currentValueUiState?.value,
                    lastUpdateTime = currentValueUiState?.lastUpdateTime,
                    min = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.NumberedValueUiState) {
                            it.minValue
                        } else {
                            null
                        }
                    },
                    minLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.NumberedValueUiState) {
                            it.minLastUpdateTime
                        } else {
                            null
                        }
                    },
                    max = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.NumberedValueUiState) {
                            it.maxValue
                        } else {
                            null
                        }
                    },
                    maxLastUpdateTime = currentValueUiState?.let {
                        if (it is HomeUnitScreenValueUiState.NumberedValueUiState) {
                            it.maxLastUpdateTime
                        } else {
                            null
                        }
                    },
                    lastTriggerSource = currentUiState.lastTriggerSource,
                    firebaseNotify = currentUiState.firebaseNotify,
                    firebaseNotifyTrigger = currentUiState.firebaseNotifyTrigger,
                    showInTaskList = currentUiState.showInTaskList,
                    /*unitsTasks = unitTaskList.value.toMutableMap().also {
                        it.remove("")
                    }*/
                )
            }
        }
    }
}
