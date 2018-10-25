package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
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

    val type: MutableLiveData<String>
    val typeList = BoardConfig.IO_HW_UNIT_TYPE_LIST

    val name: MutableLiveData<String>

    val location: MutableLiveData<String>

    // This will be only valid for Gpio type HwUnits
    val pinName: MutableLiveData<String>
    val pinNameList: MutableLiveData<List<String>> //BoardConfig.IO_GPIO_PIN_NAME_LIST

    //This should be automatically populated by selecting type
    var connectionType: MutableLiveData<ConnectionType?>

    // This is only valid for I2C type HwUnits
    var softAddress: MutableLiveData<Int?>
    var softAddressList: MutableLiveData<List<Int>>

    // This is only valid for IO_Extender type HwUnits
    var ioPin: MutableLiveData<String?>
    var ioPinNameList: MutableLiveData<List<Pair<String, Boolean>>>// MCP23017Pin.Pin.values().asList()


    // This is only valid for IO_Extender Input type HwUnits
    var pinInterrupt: MutableLiveData<String?>
    var pinInterruptList: MutableLiveData<List<Pair<String, Boolean>>>//BoardConfig.IO_EXTENDER_INT_PIN_LIST

    // This is only valid for IO_Extender Output type HwUnits
    var internalPullUp: MutableLiveData<Boolean?> = MutableLiveData()


    private val hwUnitLiveData: LiveData<HwUnit>?
    // This is for checking if given name is not already used
    private val hwUnitList: LiveData<List<HwUnit>>


    init {
        Timber.d("init hwUnitName: $hwUnitName")

        if (hwUnitName.isNotEmpty()) {
            hwUnitLiveData = homeRepository.hwUnitLiveData(hwUnitName)
            type = Transformations.map(hwUnitLiveData) { hwUnit ->
                Timber.d("init type: ${hwUnit.type}")
                hwUnit.type } as MutableLiveData<String>
            name = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.name } as MutableLiveData<String>
            location = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.location } as MutableLiveData<String>
            pinName = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.pinName } as MutableLiveData<String>
            // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
            softAddress = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.softAddress } as MutableLiveData<Int?>
            ioPin = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.ioPin } as MutableLiveData<String?>
            // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
            pinInterrupt = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.pinInterrupt } as MutableLiveData<String?>
            internalPullUp = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.internalPullUp } as MutableLiveData<Boolean?>
            connectionType = Transformations.map(hwUnitLiveData) { hwUnit ->
                Timber.d("init connectionType hwType: ${hwUnit.type}")
                when (hwUnit.type) {
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
        } else {
            hwUnitLiveData = null
            type = MutableLiveData()
            name = MutableLiveData()
            location = MutableLiveData()
            pinName = MutableLiveData()
            // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
            softAddress = MutableLiveData()
            ioPin = MutableLiveData()
            // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
            pinInterrupt = MutableLiveData()
            internalPullUp = MutableLiveData()
            connectionType = MutableLiveData()
        }



        hwUnitList = homeRepository.hwUnitListLiveData()

        pinNameList = Transformations.map(type) { hwType ->
            Timber.d("init pinNameList hwType: $hwType")
            when (hwType) {
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
            }
        } as MutableLiveData<List<String>>

        softAddressList = Transformations.map(type) { hwType ->
            Timber.d("init softAddressList hwType: $hwType")
            when (hwType) {
                BoardConfig.TEMP_SENSOR_TMP102 ->
                    BoardConfig.TEMP_SENSOR_TMP102_ADDR_LIST
                BoardConfig.TEMP_PRESS_SENSOR_BMP280 ->
                    BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR_LIST
                BoardConfig.IO_EXTENDER_MCP23017_INPUT,
                BoardConfig.IO_EXTENDER_MCP23017_OUTPUT ->
                    BoardConfig.IO_EXTENDER_MCP23017_ADDR_LIST
                else ->
                    emptyList()
            }
        } as MutableLiveData<List<Int>>

        ioPinNameList = Transformations.switchMap(type) { hwType ->
            Timber.d("init ioPinNameList hwType: $hwType")
            when (hwType) {
                BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                    Transformations.switchMap(softAddress) { softAddress ->
                        Transformations.map(hwUnitList) { hwUnitList ->
                            Timber.d("init ioPinNameList hwUnitList: $hwUnitList")
                            MCP23017Pin.Pin.values().map { pin -> Pair(pin.name, hwUnitList.find { hwUnit -> hwUnit.ioPin == pin.name && hwUnit.softAddress == softAddress} != null) }
                        }
                    }
                }
                else ->
                    MutableLiveData()
            }
        } as MutableLiveData<List<Pair<String, Boolean>>>

        pinInterruptList = Transformations.switchMap(type) { hwType ->
            Timber.d("init pinInterruptList hwType: $hwType")
            when (hwType) {
                BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                    Transformations.switchMap(softAddress) { softAddr ->
                        Timber.d("init pinInterruptList softAddr: $softAddr : ${softAddress.value}")
                        Transformations.map(hwUnitList) { hwUnitList ->
                            val addrMap = hashMapOf<Int, String>()
                            for(hwUnit: HwUnit in hwUnitList) {
                                hwUnit.softAddress?.let {addr ->
                                    hwUnit.pinInterrupt?.let {pinInt ->
                                        addrMap.put(addr, pinInt)
                                    }
                                }
                            }
                            Timber.d("init pinInterruptList addrMap: $addrMap")
                            BoardConfig.IO_EXTENDER_INT_PIN_LIST.map { intPin ->
                                Pair(intPin, !(addrMap[softAddr]?.equals(intPin) ?: !addrMap.values.contains(intPin) ))
                            }
                        }
                    }
                }
                else ->
                    MutableLiveData()
            }
        } as MutableLiveData<List<Pair<String, Boolean>>>

    }

}
