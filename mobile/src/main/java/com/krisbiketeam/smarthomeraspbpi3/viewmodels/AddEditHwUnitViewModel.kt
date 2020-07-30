package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.ui.AddEditHwUnitFragment
import timber.log.Timber

/**
 * The ViewModel used in [AddEditHwUnitFragment].
 */
class AddEditHwUnitViewModel(private val homeRepository: FirebaseHomeInformationRepository,
                             hwUnitName: String) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    private val hwUnitLiveData =
            if (hwUnitName.isNotEmpty()) homeRepository.hwUnitLiveData(hwUnitName) else null

    val typeList = BoardConfig.IO_HW_UNIT_TYPE_LIST
    val type = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.map(Transformations.distinctUntilChanged(hwUnitLiveData)) { hwUnit ->
            Timber.d("init typeItemPosition : ${typeList.indexOfFirst { it == hwUnit.type }}")
            hwUnit.type
        } as MutableLiveData<String?>
    }

    val name = if (hwUnitLiveData == null) MutableLiveData() else Transformations.map(
            hwUnitLiveData) { hwUnit -> hwUnit.name } as MutableLiveData<String>

    val location = if (hwUnitLiveData == null) MutableLiveData() else Transformations.map(
            hwUnitLiveData) { hwUnit -> hwUnit.location } as MutableLiveData<String>

    val refreshRate = if (hwUnitLiveData == null) MutableLiveData() else Transformations.map(
            hwUnitLiveData) { hwUnit -> hwUnit.refreshRate } as MutableLiveData<Long?>

    // This will be only valid for Gpio type HwUnits BoardConfig.IO_GPIO_PIN_NAME_LIST
    val pinName = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(pinNameList) { pinNameList ->
                pinNameList.firstOrNull { it == hwUnit.pinName }
            }
        } as MutableLiveData<String?>
    }
    val pinNameList: MutableLiveData<List<String>> =
            Transformations.map(Transformations.distinctUntilChanged(type)) { type ->
                Timber.d("init pinNameList type: $type")
                when (type) {
                    BoardConfig.TEMP_SENSOR_TMP102, BoardConfig.TEMP_SENSOR_MCP9808, BoardConfig.TEMP_RH_SENSOR_SI7021, BoardConfig.TEMP_PRESS_SENSOR_BMP280, BoardConfig.IO_EXTENDER_MCP23017_INPUT, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, BoardConfig.FOUR_CHAR_DISP -> BoardConfig.IO_I2C_PIN_NAME_LIST
                    BoardConfig.GPIO_INPUT, BoardConfig.GPIO_OUTPUT                                                                                                                                                                    -> BoardConfig.IO_GPIO_PIN_NAME_LIST
                    else                                                                                                                                                                                                               -> emptyList()
                }.also {
                    if (hwUnitLiveData == null) {
                        if (it.size == 1) pinName.value = it[0]
                    }
                }
            } as MutableLiveData<List<String>>

    //This should be automatically populated by selecting type
    val connectionType = Transformations.map(Transformations.distinctUntilChanged(type)) { type ->
        Timber.d("init connectionType type: $type")
        when (type) {
            BoardConfig.TEMP_SENSOR_TMP102, BoardConfig.TEMP_SENSOR_MCP9808, BoardConfig.TEMP_RH_SENSOR_SI7021, BoardConfig.TEMP_PRESS_SENSOR_BMP280, BoardConfig.IO_EXTENDER_MCP23017_INPUT, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, BoardConfig.FOUR_CHAR_DISP -> ConnectionType.I2C
            BoardConfig.GPIO_INPUT, BoardConfig.GPIO_OUTPUT                                                                                                                                                                    -> ConnectionType.GPIO
            else                                                                                                                                                                                                               -> null
        }
    } as MutableLiveData<ConnectionType?>

    // This is only valid for I2C type HwUnits
    // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
    val softAddress = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(Transformations.distinctUntilChanged(hwUnitLiveData)) { hwUnit ->
            Transformations.map(
                    Transformations.distinctUntilChanged(softAddressList)) { softAddressList ->
                softAddressList.firstOrNull {
                    it == hwUnit.softAddress
                }
            }
        } as MutableLiveData<Int?>
    }
    val softAddressList: MutableLiveData<List<Int>> =
            Transformations.map(Transformations.distinctUntilChanged(type)) { type ->
                Timber.d("init softAddressList type: $type")
                when (type) {
                    BoardConfig.TEMP_SENSOR_TMP102                                                  -> BoardConfig.TEMP_SENSOR_TMP102_ADDR_LIST
                    BoardConfig.TEMP_SENSOR_MCP9808                                                 -> BoardConfig.TEMP_SENSOR_MCP9808_ADDR_LIST
                    BoardConfig.TEMP_RH_SENSOR_SI7021                                               -> BoardConfig.TEMP_RH_SENSOR_SI7021_ADDR_LIST
                    BoardConfig.TEMP_PRESS_SENSOR_BMP280                                            -> BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR_LIST
                    BoardConfig.IO_EXTENDER_MCP23017_INPUT, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> BoardConfig.IO_EXTENDER_MCP23017_ADDR_LIST
                    else                                                                            -> emptyList()
                }.also {
                    if (hwUnitLiveData == null) {
                        if (it.size == 1) softAddress.value = it[0]
                    }
                }
            } as MutableLiveData<List<Int>>

    // This is only valid for IO_Extender type HwUnits
    val ioPinList = Transformations.switchMap(Transformations.distinctUntilChanged(type)) { type ->
        Timber.d("init ioPinList type: $type")
        when (type) {
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                Transformations.switchMap(softAddress) { softAddress ->
                    Transformations.map(hwUnitList) { hwUnitList ->
                        Timber.d("init ioPinList hwUnitList: $hwUnitList")
                        MCP23017Pin.Pin.values().map { pin ->
                            Pair(pin.name, hwUnitList.find { hwUnit ->
                                hwUnit.ioPin == pin.name && hwUnit.softAddress == softAddress
                            } != null)
                        }
                    }
                } as MutableLiveData<List<Pair<String, Boolean>>>
            }
            else                                                                            -> MutableLiveData()
        }
    } as MutableLiveData<List<Pair<String, Boolean>>>
    val ioPin = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(ioPinList) { ioPinList ->
                ioPinList.firstOrNull { it.first == hwUnit.ioPin }?.first
            }
        } as MutableLiveData<String?>
    }

    // This is only valid for IO_Extender Input type HwUnits BoardConfig.IO_EXTENDER_INT_PIN_LIST
    // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
    val pinInterruptList: MutableLiveData<List<Pair<String, Boolean>>> =
            Transformations.switchMap(Transformations.distinctUntilChanged(type)) { hwType ->
                Timber.d("init pinInterruptList hwType: $hwType")
                when (hwType) {
                    BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                        Transformations.switchMap(softAddress) { softAddr ->
                            Timber.d(
                                    "init pinInterruptList softAddr: $softAddr : ${softAddress.value}")
                            Transformations.map(hwUnitList) { hwUnitList ->
                                val addrMap = hashMapOf<Int, String>()
                                for (hwUnit: HwUnit in hwUnitList) {
                                    hwUnit.softAddress?.let { addr ->
                                        hwUnit.pinInterrupt?.let { pinInt ->
                                            addrMap.put(addr, pinInt)
                                        }
                                    }
                                }
                                Timber.d("init pinInterruptList addrMap: $addrMap")
                                BoardConfig.IO_EXTENDER_INT_PIN_LIST.map { intPin ->
                                    Pair(intPin, !(addrMap[softAddr]?.equals(intPin)
                                            ?: !addrMap.values.contains(intPin)))
                                }
                            }
                        } as MutableLiveData<List<Pair<String, Boolean>>>
                    }
                    else                                   -> MutableLiveData()
                }
            } as MutableLiveData<List<Pair<String, Boolean>>>
    val pinInterrupt = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(pinInterruptList) { pinIntList ->
                pinIntList.firstOrNull {
                    it.first == hwUnit.pinInterrupt
                }?.first
            }
        } as MutableLiveData<String?>
    }

    // This is only valid for IO_Extender Output type HwUnits
    val internalPullUp = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.map(
                hwUnitLiveData) { hwUnit -> hwUnit.internalPullUp } as MutableLiveData<Boolean?>
    }

    // This is only valid for IO_Extender Output type HwUnits
    val inverse = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.map(
                hwUnitLiveData) { hwUnit -> hwUnit.inverse } as MutableLiveData<Boolean?>
    }

    // This is for checking if given name is not already used
    private val hwUnitList = homeRepository.hwUnitListLiveData()

    private var homeRepositoryTask: Task<Void>? = null

    val showProgress: MutableLiveData<Boolean>

    init {
        Timber.d("init hwUnitName: $hwUnitName")

        if (hwUnitLiveData == null) {
            showProgress = MutableLiveData()

            showProgress.value = false
            isEditMode.value = true
        } else {
            showProgress = Transformations.map(hwUnitLiveData) { false } as MutableLiveData<Boolean>

            showProgress.value = true
            isEditMode.value = false
        }

    }

    fun noChangesMade(): Boolean {
        return hwUnitLiveData?.value?.let { unit ->
            unit.name == name.value && unit.location == location.value && unit.type == type.value && unit.pinName == pinName.value && unit.connectionType == connectionType.value && unit.softAddress == softAddress.value && unit.pinInterrupt == pinInterrupt.value && unit.ioPin == ioPin.value && unit.internalPullUp == internalPullUp.value && unit.inverse == inverse.value && unit.refreshRate == refreshRate.value
        } ?: name.value.isNullOrEmpty()
        /*?: type.value.isNullOrEmpty()
        ?: roomName.value.isNullOrEmpty()
        ?: hwUnitName.value.isNullOrEmpty()*/ ?: true
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [AddEditHwUnitFragment]
     */
    fun actionDiscard(): Boolean {
        return if (hwUnitLiveData == null) {
            true
        } else {
            isEditMode.value = false
            hwUnitLiveData.value?.let { unit ->
                name.value = unit.name
                location.value = unit.location
                type.value = unit.type
                pinName.value = unit.pinName
                // connectionType  is automatically populated by type LiveData
                // TODO: how to handle it like type
                softAddress.value = when (unit.type) {
                    BoardConfig.TEMP_SENSOR_TMP102                                                  -> BoardConfig.TEMP_SENSOR_TMP102_ADDR_LIST
                    BoardConfig.TEMP_SENSOR_MCP9808                                                 -> BoardConfig.TEMP_SENSOR_MCP9808_ADDR_LIST
                    BoardConfig.TEMP_RH_SENSOR_SI7021                                               -> BoardConfig.TEMP_RH_SENSOR_SI7021_ADDR_LIST
                    BoardConfig.TEMP_PRESS_SENSOR_BMP280                                            -> BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR_LIST
                    BoardConfig.IO_EXTENDER_MCP23017_INPUT, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> BoardConfig.IO_EXTENDER_MCP23017_ADDR_LIST
                    else                                                                            -> emptyList()
                }.indexOfFirst {
                    it == unit.softAddress
                }
                //softAddress.value = unit.softAddress
                pinInterrupt.value = unit.pinInterrupt
                ioPin.value = unit.ioPin
                internalPullUp.value = unit.internalPullUp
                inverse.value = unit.inverse
            }
            false
        }
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with
     * this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewHwUnit?: ${hwUnitLiveData == null} name.value: ${name.value}")
        when {
            type.value.isNullOrEmpty()                                                                                                                                   -> return Pair(
                    R.string.add_edit_hw_unit_empty_type, null)
            name.value?.trim()
                    .isNullOrEmpty()                                                                                                                                     -> return Pair(
                    R.string.add_edit_hw_unit_empty_name, null)
            (hwUnitLiveData == null || (hwUnitLiveData.value?.name != name.value?.trim())) && hwUnitList.value?.find { unit -> unit.name == name.value?.trim() } != null -> {
                //This name is already used
                Timber.d("This name is already used")
                return Pair(R.string.add_edit_hw_unit_name_already_used, null)
            }
            location.value?.trim()
                    .isNullOrEmpty()                                                                                                                                     -> return Pair(
                    R.string.add_edit_hw_unit_empty_location, null)
            pinName.value.isNullOrEmpty()                                                                                                                                -> return Pair(
                    R.string.add_edit_hw_unit_empty_pin_name, null)
            connectionType.value == ConnectionType.I2C && softAddress.value == null                                                                                      -> return Pair(
                    R.string.add_edit_hw_unit_empty_soft_address, null)
            (type.value == BoardConfig.IO_EXTENDER_MCP23017_INPUT || type.value == BoardConfig.IO_EXTENDER_MCP23017_OUTPUT) && ioPin.value == null                       -> return Pair(
                    R.string.add_edit_hw_unit_empty_io_pin, null)
            type.value == BoardConfig.IO_EXTENDER_MCP23017_INPUT && pinInterrupt.value == null                                                                           -> return Pair(
                    R.string.add_edit_hw_unit_empty_pin_interrupt, null)

            hwUnitLiveData != null                                                                                                                                       -> // Editing existing HomeUnit
                hwUnitLiveData.value?.let { unit ->
                    return when {
                        name.value?.trim() != unit.name -> Pair(
                                R.string.add_edit_hw_unit_save_with_delete, R.string.overwrite)
                        noChangesMade()                 -> Pair(
                                R.string.add_edit_home_unit_no_changes, null)
                        else                            -> Pair(
                                R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                    }
                }
        }
        // new Hw Unit adding just show Save Dialog
        return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }

    fun deleteHomeUnit(): Task<Void>? {
        Timber.d(
                "deleteHwUnit homeUnit: ${hwUnitLiveData?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = hwUnitLiveData?.value?.let { unit ->
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
                "saveChanges hwUnitLiveData: ${hwUnitLiveData?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = hwUnitLiveData?.value?.let { unit ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unit.name) {
                    Timber.d("Name changed will need to delete old value name=${name.value}")
                    // delete old HomeUnit
                    this?.continueWithTask { homeRepository.deleteHardwareUnit(unit) }
                }
            }
        } ?: doSaveChanges()?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        return name.value?.let { name ->
            location.value?.let { location ->
                type.value?.let { type ->
                    pinName.value?.let { pinName ->
                        showProgress.value = true
                        homeRepository.saveHardwareUnit(
                                HwUnit(name = name, location = location, type = type,
                                       pinName = pinName, connectionType = connectionType.value,
                                       softAddress = softAddress.value,
                                       pinInterrupt = pinInterrupt.value, ioPin = ioPin.value,
                                       internalPullUp = internalPullUp.value,
                                       inverse = inverse.value, refreshRate = refreshRate.value))
                    }
                }
            }
        }
    }
}
