package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig.IO_HW_UNIT_TYPE_LIST
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.ui.AddEditHwUnitFragment
import timber.log.Timber

/**
 * The ViewModel used in [AddEditHwUnitFragment].
 */
class AddEditHwUnitViewModel(
        homeRepository: HomeInformationRepository
) : ViewModel() {

    var type: MutableLiveData<String?> = MutableLiveData()
    val typeList = IO_HW_UNIT_TYPE_LIST

    var name: MutableLiveData<String> = MutableLiveData()

    var location: MutableLiveData<String> = MutableLiveData()

    // This will be only valid for Gpio type HwUnits
    var pinName: MutableLiveData<String> = MutableLiveData()
    val pinNameList = BoardConfig.IO_GPIO_PIN_NAME_LIST

    //This should be automatically populated by selecting type
    var connectionType: MutableLiveData<ConnectionType?> = MutableLiveData()

    // This is only valid for I2C type HwUnits
    var softAddress: MutableLiveData<Int?> = MutableLiveData()

    // This is only valid for IO_Extender type HwUnits
    var pinInterrupt: MutableLiveData<String?> =  MutableLiveData()
    val ioPinInterruptList = BoardConfig.IO_EXTENDER_INT_PIN_LIST

    // This is only valid for IO_Extender type HwUnits
    var ioPin: MutableLiveData<String?> = MutableLiveData()
    val ioPinNameList = MCP23017Pin.Pin.values().asList()

    // This is only valid for IO_Extender type HwUnits
    val internalPullUp: MutableLiveData<Boolean?> = MutableLiveData()

    // This is for checking if given name is not already used
    private val hwUnitList: LiveData<List<HwUnit>>



    init {
        Timber.d("init")
        type.value = ""

        hwUnitList = homeRepository.hwUnitListLiveData()
        /*pinNameList = Transformations.map(homeRepository.roomsLiveData()){ list ->
            list.map {it.name}
        }
        hardwareUnitNameList =Transformations.map(homeRepository.hwUnitListLiveData()){ list ->
            list.map {it.name}
        }
        homeUnitListLiveData = Transformations.switchMap(location){ tableName ->
            Transformations.map(homeRepository.homeUnitListLiveData(tableName)){ homeUnitList ->
                homeUnitList.map {it.name}
            }
        }*/
    }

}
