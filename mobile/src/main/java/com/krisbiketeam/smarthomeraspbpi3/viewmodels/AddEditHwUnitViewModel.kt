package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig.GPIO_HW_UNIT_LIST
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig.I2C_HW_UNIT_LIST
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.ui.AddEditHwUnitFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The ViewModel used in [AddEditHwUnitFragment].
 */
@ExperimentalCoroutinesApi
class AddEditHwUnitViewModel(private val homeRepository: FirebaseHomeInformationRepository,
                             hwUnitName: String) : ViewModel() {


    private val hwUnit: StateFlow<HwUnit?> =
            homeRepository.hwUnitFlow(hwUnitName).onEach { hwUnit ->
                Timber.e("hwUnit changed:$hwUnit")
                showProgress.value = false

                type.value = hwUnit.type
                name.value = hwUnit.name
                location.value = hwUnit.location
                refreshRate.value = hwUnit.refreshRate
                pinName.value = hwUnit.pinName
                // connectionType will be set automatically from type
                softAddress.value = hwUnit.softAddress
                ioPin.value = hwUnit.ioPin
                pinInterrupt.value = hwUnit.pinInterrupt
                internalPullUp.value = hwUnit.internalPullUp
                inverse.value = hwUnit.inverse
            }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // This is for checking if given name is not already used
    private val hwUnitList: StateFlow<List<HwUnit>> = homeRepository.hwUnitListFlow().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var homeRepositoryTask: Task<Void>? = null


    val showProgress: MutableStateFlow<Boolean> = MutableStateFlow(hwUnitName.isNotEmpty())

    val isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(hwUnitName.isEmpty())

    val typeList = BoardConfig.IO_HW_UNIT_TYPE_LIST
    val type: MutableStateFlow<String?> = MutableStateFlow(null)

    val name: MutableStateFlow<String> = MutableStateFlow(hwUnitName)

    val location: MutableStateFlow<String> = MutableStateFlow("")

    val refreshRate: MutableStateFlow<Long?> = MutableStateFlow(null)

    // This will be only valid for Gpio type HwUnits BoardConfig.IO_GPIO_PIN_NAME_LIST
    val pinNameList: StateFlow<List<String>> =
            combine(type, hwUnit) { type, hwUnit ->
                Timber.d("init pinNameList type: $type")
                when {
                    I2C_HW_UNIT_LIST.contains(type) -> BoardConfig.IO_I2C_PIN_NAME_LIST
                    GPIO_HW_UNIT_LIST.contains(type) -> BoardConfig.IO_GPIO_PIN_NAME_LIST
                    else -> emptyList()
                }.also { pinNameList ->
                    if (hwUnit == null) {
                        if (pinNameList.size == 1) pinName.value = pinNameList[0]
                    } else {
                        pinName.value = pinNameList.firstOrNull { it == hwUnit.pinName }
                    }
                }
            }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val pinName: MutableStateFlow<String?> = MutableStateFlow(null)

    //This should be automatically populated by selecting type
    val connectionType = type.map { type ->
        Timber.d("init connectionType type: $type")
        when {
            I2C_HW_UNIT_LIST.contains(type) -> ConnectionType.I2C
            GPIO_HW_UNIT_LIST.contains(type) -> ConnectionType.GPIO
            else -> null
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // This is only valid for I2C type HwUnits
    // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
    val softAddressList: StateFlow<List<Int>> =
            combine(type, hwUnit) { type, hwUnit ->
                Timber.d("init softAddressList type: $type")
                when (type) {
                    BoardConfig.TEMP_SENSOR_TMP102 -> BoardConfig.TEMP_SENSOR_TMP102_ADDR_LIST
                    BoardConfig.TEMP_SENSOR_MCP9808 -> BoardConfig.TEMP_SENSOR_MCP9808_ADDR_LIST
                    BoardConfig.TEMP_RH_SENSOR_SI7021 -> BoardConfig.TEMP_RH_SENSOR_SI7021_ADDR_LIST
                    BoardConfig.TEMP_RH_SENSOR_AM2320 -> BoardConfig.TEMP_RH_SENSOR_AM2320_ADDR_LIST
                    BoardConfig.AIR_QUALITY_SENSOR_BME680 -> BoardConfig.AIR_QUALITY_SENSOR_BME680_ADDR_LIST
                    BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR_LIST
                    BoardConfig.IO_EXTENDER_MCP23017_INPUT, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> BoardConfig.IO_EXTENDER_MCP23017_ADDR_LIST
                    else -> emptyList()
                }.also { softAddressList ->
                    if (hwUnit == null) {
                        if (softAddressList.size == 1) softAddress.value = softAddressList[0]
                    } else {
                        softAddress.value = softAddressList.firstOrNull { it == hwUnit.softAddress }
                    }
                }
            }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val softAddress: MutableStateFlow<Int?> = MutableStateFlow(null)

    // This is only valid for IO_Extender type HwUnits
    val ioPinList: StateFlow<List<Pair<String, Boolean>>> = combine(type, softAddress, hwUnitList, hwUnit) { type, softAddress, hwUnitList, hwUnit ->
        Timber.d("init ioPinList type: $type hwUnitList: $hwUnitList softAddress: $softAddress hwUnit:$hwUnit")
        when (type) {
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                MCP23017Pin.Pin.values().map { pin ->
                    if (pin.name == hwUnit?.ioPin) {
                        ioPin.value = pin.name
                    }
                    Pair(pin.name, hwUnitList.find { hwUnit ->
                        hwUnit.ioPin == pin.name && hwUnit.softAddress == softAddress
                    } != null)
                }
            }
            else -> emptyList()
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val ioPin: MutableStateFlow<String?> = MutableStateFlow(null)

    // This is only valid for IO_Extender Input type HwUnits BoardConfig.IO_EXTENDER_INT_PIN_LIST
    // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
    val pinInterruptList: StateFlow<List<Pair<String, Boolean>>> = combine(type, softAddress, hwUnitList, hwUnit) { hwType, softAddr, hwUnitList, hwUnit ->
        Timber.d("init pinInterruptList hwType: $hwType")
        when (hwType) {
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                Timber.d(
                        "init pinInterruptList softAddr: $softAddr : ${softAddress.value}")
                val addrMap = hashMapOf<Int, String>()
                for (listHwUnit: HwUnit in hwUnitList) {
                    listHwUnit.softAddress?.let { addr ->
                        listHwUnit.pinInterrupt?.let { pinInt ->
                            addrMap.put(addr, pinInt)
                        }
                    }
                }
                Timber.d("init pinInterruptList addrMap: $addrMap")
                BoardConfig.IO_EXTENDER_INT_PIN_LIST.map { intPin ->
                    if (intPin == hwUnit?.pinInterrupt) {
                        pinInterrupt.value = intPin
                    }
                    Pair(intPin, !(addrMap[softAddr]?.equals(intPin)
                            ?: !addrMap.values.contains(intPin)))
                }
            }
            else -> emptyList()
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val pinInterrupt: MutableStateFlow<String?> = MutableStateFlow(null)

    // This is only valid for IO_Extender Output type HwUnits
    val internalPullUp: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    // This is only valid for IO_Extender Output type HwUnits
    val inverse: MutableStateFlow<Boolean?> = MutableStateFlow(null)


    fun noChangesMade(): Boolean {
        return hwUnit.value?.let { unit ->
            unit.name == name.value
                    && unit.location == location.value
                    && unit.type == type.value
                    && unit.pinName == pinName.value
                    && unit.connectionType == connectionType.value
                    && unit.softAddress == softAddress.value
                    && unit.pinInterrupt == pinInterrupt.value
                    && unit.ioPin == ioPin.value
                    && unit.internalPullUp == internalPullUp.value
                    && unit.inverse == inverse.value
                    && unit.refreshRate == refreshRate.value
        } ?: true
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [AddEditHwUnitFragment]
     */
    fun actionDiscard(): Boolean {
        isEditMode.value = false
        return hwUnit.value?.let { unit ->
            name.value = unit.name
            location.value = unit.location
            type.value = unit.type
            pinName.value = unit.pinName
            // connectionType.value will be reset by changing type
            softAddress.value = unit.softAddress
            pinInterrupt.value = unit.pinInterrupt
            ioPin.value = unit.ioPin
            internalPullUp.value = unit.internalPullUp
            inverse.value = unit.inverse
            refreshRate.value = unit.refreshRate
            false
        } ?: true
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with
     * this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewHwUnit?: ${hwUnit.value == null} name.value: ${name.value}")
        when {
            type.value.isNullOrEmpty() -> return Pair(
                    R.string.add_edit_hw_unit_empty_type, null)
            name.value.trim().isEmpty() -> return Pair(
                    R.string.add_edit_hw_unit_empty_name, null)
            (hwUnit.value == null || (hwUnit.value?.name != name.value.trim())) && hwUnitList.value.find { unit -> unit.name == name.value.trim() } != null -> {
                //This name is already used
                Timber.d("This name is already used")
                return Pair(R.string.add_edit_hw_unit_name_already_used, null)
            }
            location.value.trim().isEmpty() -> return Pair(
                    R.string.add_edit_hw_unit_empty_location, null)
            pinName.value.isNullOrEmpty() -> return Pair(
                    R.string.add_edit_hw_unit_empty_pin_name, null)
            connectionType.value == ConnectionType.I2C && softAddress.value == null -> return Pair(
                    R.string.add_edit_hw_unit_empty_soft_address, null)
            (type.value == BoardConfig.IO_EXTENDER_MCP23017_INPUT || type.value == BoardConfig.IO_EXTENDER_MCP23017_OUTPUT) && ioPin.value == null -> return Pair(
                    R.string.add_edit_hw_unit_empty_io_pin, null)
            type.value == BoardConfig.IO_EXTENDER_MCP23017_INPUT && pinInterrupt.value == null -> return Pair(
                    R.string.add_edit_hw_unit_empty_pin_interrupt, null)

            hwUnit.value != null -> // Editing existing HomeUnit
                hwUnit.value?.let { unit ->
                    return when {
                        name.value.trim() != unit.name -> Pair(
                                R.string.add_edit_hw_unit_save_with_delete, R.string.overwrite)
                        noChangesMade() -> Pair(
                                R.string.add_edit_home_unit_no_changes, null)
                        else -> Pair(
                                R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                    }
                }
        }
        // new Hw Unit adding just show Save Dialog
        return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }

    fun deleteHomeUnit(): Task<Void>? {
        Timber.d(
                "deleteHwUnit homeUnit: ${hwUnit.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = hwUnit.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteHardwareUnit(unit)
        }?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        Timber.d(
                "saveChanges hwUnitLiveData: ${hwUnit.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = hwUnit.value?.let { unit ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unit.name) {
                    Timber.d("Name changed will need to delete old value name=${name.value}")
                    // delete old HomeUnit
                    this?.continueWithTask { homeRepository.deleteHardwareUnit(unit) ?: it }
                }
            }
        } ?: doSaveChanges()?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        return type.value?.let { type ->
            pinName.value?.let { pinName ->
                showProgress.value = true
                homeRepository.saveHardwareUnit(
                        HwUnit(name = name.value, location = location.value, type = type,
                                pinName = pinName, connectionType = connectionType.value,
                                softAddress = softAddress.value,
                                pinInterrupt = pinInterrupt.value, ioPin = ioPin.value,
                                internalPullUp = internalPullUp.value,
                                inverse = inverse.value, refreshRate = refreshRate.value))
            }
        }
    }
}
