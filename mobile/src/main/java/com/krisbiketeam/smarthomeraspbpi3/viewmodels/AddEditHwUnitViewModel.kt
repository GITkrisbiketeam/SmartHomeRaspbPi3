package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.ui.AddEditHwUnitFragment
import timber.log.Timber

/**
 * The ViewModel used in [AddEditHwUnitFragment].
 */
class AddEditHwUnitViewModel(
        homeRepository: HomeInformationRepository,
        hwUnitName: String
) : ViewModel() {

    private val hwUnitLiveData = if(hwUnitName.isNotEmpty()) homeRepository.hwUnitLiveData(hwUnitName) else null

    val typeItemPosition = if(hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.map(hwUnitLiveData) { hwUnit ->
            Timber.d("init typeItemPosition : ${typeList.indexOfFirst { it == hwUnit.type }}")
            typeList.indexOfFirst { it == hwUnit.type }
        } as MutableLiveData<Int>
    }
    val typeList = BoardConfig.IO_HW_UNIT_TYPE_LIST
    val type = Transformations.map(typeItemPosition) { typePos ->
        if (typePos in 0 until typeList.size) {
            Timber.d("type getValue position: $typePos val: ${typeList[typePos]}")
            typeList[typePos]
        } else {
            Timber.d("type getValue position: $typePos val: null")
            null
        }
    } as MutableLiveData<String?>

    val name = if(hwUnitLiveData == null) MutableLiveData() else Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.name } as MutableLiveData<String>

    val location = if(hwUnitLiveData == null) MutableLiveData() else Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.location } as MutableLiveData<String>

    // This will be only valid for Gpio type HwUnits BoardConfig.IO_GPIO_PIN_NAME_LIST
    val pinNamePosition = if(hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(pinNameList) { pinNameList ->
                pinNameList.indexOfFirst {
                    it == hwUnit.pinName
                }
            }
        } as MutableLiveData<Int>
    }
    val pinNameList: MutableLiveData<List<String>> = Transformations.map(type) { type ->
        Timber.d("init pinNameList type: $type")
        when (type) {
            BoardConfig.TEMP_SENSOR_TMP102,
            BoardConfig.TEMP_PRESS_SENSOR_BMP280,
            BoardConfig.IO_EXTENDER_MCP23017_INPUT,
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
            BoardConfig.FOUR_CHAR_DISP ->
                BoardConfig.IO_I2C_PIN_NAME_LIST
            BoardConfig.GPIO_INPUT,
            BoardConfig.GPIO_OUTPUT ->
                BoardConfig.IO_GPIO_PIN_NAME_LIST
            else ->
                emptyList()
        }.also {
            if (hwUnitName.isEmpty()) {pinNamePosition.value = it.size}
        }
    } as MutableLiveData<List<String>>
    var pinName
        get() = pinNamePosition.value?.let { position ->
            Timber.d("pinName getValue position: $position val: ${pinNameList.value?.get(position)}")
            pinNameList.value?.get(position)
        }
        set(value) {
            val position = pinNameList.value?.indexOfFirst {
                it == value
            } ?: -1
            Timber.d("pinName setValue val: $value position: $position")
            if (position != -1) {
                pinNamePosition.value = position
            }
        }

    //This should be automatically populated by selecting type
    val connectionType = Transformations.map(type) { type ->
        Timber.d("init connectionType type: $type")
        when (type) {
            BoardConfig.TEMP_SENSOR_TMP102,
            BoardConfig.TEMP_PRESS_SENSOR_BMP280,
            BoardConfig.IO_EXTENDER_MCP23017_INPUT,
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
            BoardConfig.FOUR_CHAR_DISP ->
                ConnectionType.I2C
            BoardConfig.GPIO_INPUT,
            BoardConfig.GPIO_OUTPUT ->
                ConnectionType.GPIO
            else ->
                null
        }
    } as MutableLiveData<ConnectionType?>

    // This is only valid for I2C type HwUnits
    // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
    val softAddressPosition = if(hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(softAddressList) { softAddressList ->
                softAddressList.indexOfFirst {
                    it == hwUnit.softAddress
                }
            }
        } as MutableLiveData<Int>
    }
    val softAddressList: MutableLiveData<List<Int>> = Transformations.map(type) { type ->
        Timber.d("init softAddressList type: $type")
        when (type) {
            BoardConfig.TEMP_SENSOR_TMP102 ->
                BoardConfig.TEMP_SENSOR_TMP102_ADDR_LIST
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 ->
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR_LIST
            BoardConfig.IO_EXTENDER_MCP23017_INPUT,
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT ->
                BoardConfig.IO_EXTENDER_MCP23017_ADDR_LIST
            else ->
                emptyList()
        }.also {
            if (hwUnitName.isEmpty()) {softAddressPosition.value = it.size}
        }
    } as MutableLiveData<List<Int>>
    val softAddress: MutableLiveData<Int?> = Transformations.switchMap(softAddressPosition) { softAddressPos ->
        Transformations.map(softAddressList) { softAddrList ->
            if (softAddressPos in 0 until softAddrList.size) {
                Timber.d("softAddress getValue position: $softAddressPos val: ${softAddrList[softAddressPos]}")
                softAddrList[softAddressPos]
            } else {
                Timber.d("softAddress getValue position: $softAddressPos val: null")
                null
            }
        }
    } as MutableLiveData<Int?>

    // This is only valid for IO_Extender type HwUnits
    val ioPinList = Transformations.switchMap(type) { type ->
        Timber.d("init ioPinList type: $type")
        when (type) {
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                Transformations.switchMap(softAddress) { softAddress ->
                    Transformations.map(hwUnitList) { hwUnitList ->
                        Timber.d("init ioPinList hwUnitList: $hwUnitList")
                        MCP23017Pin.Pin.values().map { pin ->
                            Pair(pin.name, hwUnitList.find { hwUnit ->
                                hwUnit.ioPin == pin.name && hwUnit.softAddress == softAddress
                            } != null)
                        }
                    }
                }
            }
            else -> MutableLiveData()
        }
    } as MutableLiveData<List<Pair<String, Boolean>>>
    val ioPinPosition = if (hwUnitLiveData == null) {
        Transformations.map(ioPinList) { ioPinList ->
            ioPinList.size
        } as MutableLiveData<Int>
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(ioPinList) { ioPinList ->
                ioPinList.indexOfFirst {
                    it.first == hwUnit.ioPin
                }
            }
        } as MutableLiveData<Int>
    }
    var ioPin
        get() =
            ioPinPosition.value?.let {
                ioPinList.value?.get(it)?.first
            }
        set(value) {
            val position = ioPinList.value?.indexOfFirst {
                it.first == value
            } ?: -1
            if (position != -1) {
                ioPinPosition.value = position
            }
        }


    // This is only valid for IO_Extender Input type HwUnits BoardConfig.IO_EXTENDER_INT_PIN_LIST
    // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
    val pinInterruptList: MutableLiveData<List<Pair<String, Boolean>>> = Transformations.switchMap(type) { hwType ->
        Timber.d("init pinInterruptList hwType: $hwType")
        when (hwType) {
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                Transformations.switchMap(softAddress) { softAddr ->
                    Timber.d("init pinInterruptList softAddr: $softAddr : ${softAddress.value}")
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
                }
            }
            else ->
                MutableLiveData()
        }
    } as MutableLiveData<List<Pair<String, Boolean>>>
    val pinInterruptItemPosition = if(hwUnitLiveData == null) {
        Transformations.map(pinInterruptList) { ioPinList ->
            ioPinList.size
        } as MutableLiveData<Int>
    } else {
        Transformations.switchMap(hwUnitLiveData) { hwUnit ->
            Transformations.map(pinInterruptList) { pinIntList ->
                pinIntList.indexOfFirst {
                    it.first == hwUnit.pinInterrupt
                }
            }
        } as MutableLiveData<Int>
    }
    var pinInterrupt
        get() =
            pinInterruptItemPosition.value?.let {
                pinInterruptList.value?.get(it)?.first
            }
        set(value) {
            val position = pinInterruptList.value?.indexOfFirst {
                it.first == value
            } ?: -1
            if (position != -1) {
                pinInterruptItemPosition.value = position
            }
        }

    // This is only valid for IO_Extender Output type HwUnits
    val internalPullUp = if (hwUnitLiveData == null) {
        MutableLiveData()
    } else {
        Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.internalPullUp } as MutableLiveData<Boolean?>
    }

    // This is for checking if given name is not already used
    private val hwUnitList = homeRepository.hwUnitListLiveData()

    private val addingNewHwUnit = hwUnitName.isEmpty()

    init {
        Timber.d("init hwUnitName: $hwUnitName")

        if (hwUnitLiveData == null){
            typeItemPosition.value = typeList.size
        }
    }
    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewHwUnit: $addingNewHwUnit name.value: ${name.value}")
        /*if (addingNewHwUnit) {
            // Adding new HomeUnit
            when {
                name.value?.trim().isNullOrEmpty() -> return Pair(R.string.add_edit_home_unit_empty_name, null)
                type.value?.trim().isNullOrEmpty() -> return Pair(R.string.add_edit_home_unit_empty_unit_type, null)
                hwUnitName.value?.trim().isNullOrEmpty() -> return Pair(R.string.add_edit_home_unit_empty_unit_hw_unit, null)
                homeUnitList.value?.find { unit -> unit.name == name.value?.trim() } != null ->
                {
                    //This name is already used
                    Timber.d("This name is already used")
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                }
            }
        } else {
            // Editing existing HomeUnit
            homeUnit?.value?.let { unit ->
                return if (name.value?.trim().isNullOrEmpty()) {
                    Pair(R.string.add_edit_home_unit_empty_name, null)
                } else if (name.value?.trim() != unit.name || type.value?.trim() != unit.type) {
                    Pair(R.string.add_edit_home_unit_save_with_delete, R.string.overwrite)
                } else if (noChangesMade()) {
                    Pair(R.string.add_edit_home_unit_no_changes, null)
                } else {
                    Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                }
            }
        }*/
        // new Home Unit adding just show Save Dialog
        return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }
}
