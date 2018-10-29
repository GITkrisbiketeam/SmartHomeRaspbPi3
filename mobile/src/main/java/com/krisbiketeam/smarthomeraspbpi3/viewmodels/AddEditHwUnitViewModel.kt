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

    var type: MutableLiveData<String?>/* = object : MutableLiveData<String?>() {
        override fun setValue(value: String?) {
            val position = typeList.indexOfFirst {
                it == value
            }
            Timber.d("type setValue val: $value position: $position")
            if (position != -1) {
                typeItemPosition.value = position
            }
        }
        override fun postValue(value: String?) {
            val position = typeList.indexOfFirst {
                it == value
            }
            Timber.d("type postValue val: $value position: $position")
            if (position != -1) {
                typeItemPosition.value = position
            }
        }

        override fun getValue(): String? {
            return typeItemPosition.value?.let {position ->
                if (position != -1) {
                    Timber.d("type getValue position: $position val: ${typeList.get(position)}")
                    typeList.get(position)
                } else {
                    Timber.d("type getValue position: $position val: null")
                    null
                }
            }
        }
    }*/
    val typeList = BoardConfig.IO_HW_UNIT_TYPE_LIST
    var typeItemPosition: MutableLiveData<Int>

    val name: MutableLiveData<String>

    val location: MutableLiveData<String>

    // This will be only valid for Gpio type HwUnits
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
    var pinNamePosition = MutableLiveData<Int>()
    lateinit var pinNameList: MutableLiveData<List<String>> //BoardConfig.IO_GPIO_PIN_NAME_LIST

    //This should be automatically populated by selecting type
    var connectionType: MutableLiveData<ConnectionType?>

    // This is only valid for I2C type HwUnits
    var softAddress: MutableLiveData<Int?>
    var softAddressPosition = MutableLiveData<Int>()
    lateinit var softAddressList: MutableLiveData<List<Int>>

    // This is only valid for IO_Extender type HwUnits
    var ioPin: MutableLiveData<String?>
    var ioPinNameList: MutableLiveData<List<Pair<String, Boolean>>>// MCP23017Pin.Pin.values().asList()


    // This is only valid for IO_Extender Input type HwUnits
    var pinInterrupt: String?
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

    lateinit var pinInterruptList: MutableLiveData<List<Pair<String, Boolean>>>//BoardConfig.IO_EXTENDER_INT_PIN_LIST
    var pinInterruptItemPosition = MutableLiveData<Int>()

    // This is only valid for IO_Extender Output type HwUnits
    var internalPullUp: MutableLiveData<Boolean?> = MutableLiveData()


    private val hwUnitLiveData: LiveData<HwUnit>?
    // This is for checking if given name is not already used
    private val hwUnitList: LiveData<List<HwUnit>>


    init {
        Timber.d("init hwUnitName: $hwUnitName")

        if (hwUnitName.isNotEmpty()) {
            hwUnitLiveData = homeRepository.hwUnitLiveData(hwUnitName)

            typeItemPosition = Transformations.map(hwUnitLiveData) { hwUnit ->
                Timber.d("init typeItemPosition : ${typeList.indexOfFirst { it == hwUnit.type }}")
                //type.value = hwUnit.type
                typeList.indexOfFirst { it == hwUnit.type }
            } as MutableLiveData<Int>
            name = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.name } as MutableLiveData<String>
            location = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.location } as MutableLiveData<String>
            pinNamePosition = Transformations.map(hwUnitLiveData) { hwUnit -> pinNameList.value?.indexOfFirst { it == hwUnit.pinName } } as MutableLiveData<Int>
            // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
            softAddressPosition = Transformations.map(hwUnitLiveData) { hwUnit -> softAddressList.value?.indexOfFirst { it == hwUnit.softAddress } } as MutableLiveData<Int>
            ioPin = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.ioPin } as MutableLiveData<String?>
            // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
            //pinInterrupt = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.pinInterrupt } as MutableLiveData<String?>
            pinInterruptItemPosition = Transformations.switchMap(hwUnitLiveData) { hwUnit ->
                Transformations.map(pinInterruptList) { pinIntList ->
                    pinInterruptList.value?.indexOfFirst {
                        it.first == hwUnit.pinInterrupt
                    } ?: -1
                }
            } as MutableLiveData<Int>
            internalPullUp = Transformations.map(hwUnitLiveData) { hwUnit -> hwUnit.internalPullUp } as MutableLiveData<Boolean?>

        } else {
            hwUnitLiveData = null
            //type = MutableLiveData()
            typeItemPosition = MutableLiveData()
            name = MutableLiveData()
            location = MutableLiveData()
            pinNamePosition = MutableLiveData()
            // This softAddress should be strictly linked to [pinInterrupt] as this pair makes a physical MCP23017 unit
            softAddressPosition = MutableLiveData()
            ioPin = MutableLiveData()
            // This pinInterrupt should be strictly linked to [softAddress] as this pair makes a physical MCP23017 unit
            //pinInterrupt = MutableLiveData()
            pinInterruptItemPosition = MutableLiveData()
            internalPullUp = MutableLiveData()

            typeItemPosition.value = typeList.size
        }

        type = Transformations.map(typeItemPosition) { typePos ->
            if (typePos in 0 until typeList.size) {
                Timber.d("type getValue position: $typePos val: ${typeList[typePos]}")
                typeList[typePos]
            } else {
                Timber.d("type getValue position: $typePos val: null")
                null
            }
        } as MutableLiveData<String?>

        softAddress = Transformations.switchMap(softAddressPosition) { softAddressPos ->
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

        connectionType = Transformations.map(type) { type ->
            Timber.d("init connectionType type: ${type}")
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

        hwUnitList = homeRepository.hwUnitListLiveData()

        pinNameList = Transformations.map(type) { type ->
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

        softAddressList = Transformations.map(type) { type ->
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

        ioPinNameList = Transformations.switchMap(type) { type ->
            Timber.d("init ioPinNameList type: $type")
            when (type) {
                BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                    Transformations.switchMap(softAddress) { softAddress ->
                        Transformations.map(hwUnitList) { hwUnitList ->
                            Timber.d("init ioPinNameList hwUnitList: $hwUnitList")
                            MCP23017Pin.Pin.values().map { pin -> Pair(pin.name, hwUnitList.find { hwUnit -> hwUnit.ioPin == pin.name && hwUnit.softAddress == softAddress } != null) }
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

    }

}
