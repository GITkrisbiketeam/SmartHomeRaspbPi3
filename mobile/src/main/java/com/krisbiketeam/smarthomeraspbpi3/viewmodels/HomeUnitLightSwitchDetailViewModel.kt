package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitLightSwitchDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * The ViewModel used in [HomeUnitLightSwitchDetailFragment].
 */
@ExperimentalCoroutinesApi
class HomeUnitLightSwitchDetailViewModel(
    application: Application,
    homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, unitName: String?
) : HomeUnitDetailViewModelBase<LightSwitchHomeUnit<Any>>(
    application,
    homeRepository,
    roomName,
    unitName,
    HomeUnitType.HOME_LIGHT_SWITCHES_V2
) {

    val switchHwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val switchValue: MutableStateFlow<String> = MutableStateFlow("")
    val switchLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    override fun getHomeUnitFlow(unitType: HomeUnitType, unitName: String) =
        homeRepository.lightSwitchHomeUnitFlow(unitName)

    override fun initializeAdditionalHomeUnitStates(homeUnit: LightSwitchHomeUnit<Any>) {
        switchHwUnitName.value = homeUnit.switchHwUnitName
        switchValue.value = homeUnit.switchValue.toString()
        switchLastUpdateTime.value =
            getLastUpdateTime(getApplication(), homeUnit.switchLastUpdateTime)
    }

    fun setValueFromSwitch(isChecked: Boolean): Task<Void>? {
        Timber.d("OnCheckedChangeListener isChecked: $isChecked")
        return if (homeUnit?.value?.value != isChecked) {
            homeUnit?.value?.copy()?.let { unit ->
                unit.value = isChecked
                unit.lastUpdateTime = System.currentTimeMillis()
                unit.lastTriggerSource = LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
                homeRepository.updateHomeUnitValue(unit)
            }
        } else {
            null
        }
    }

    override fun additionalNoChangesMade(homeUnit: LightSwitchHomeUnit<Any>): Boolean {
        return homeUnit.switchHwUnitName == switchHwUnitName.value
    }

    override fun restoreAdditionalHomeUnitInitialStates(homeUnit: LightSwitchHomeUnit<Any>) {
        switchHwUnitName.value = homeUnit.switchHwUnitName
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    override fun actionSaveGetCustomSavePair(): Pair<Int, Int?>? {
        return when {
            hwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_hw_unit, null
            )
            switchHwUnitName.value?.trim().isNullOrEmpty() -> return Pair(
                R.string.add_edit_home_unit_empty_unit_second_hw_unit, null
            )
            else -> null
        }
    }

    override fun getHomeUnitToSave(): HomeUnit<Any> {
        return LightSwitchHomeUnit(
            name = name.value,
            type = type.value,
            room = room.value,
            hwUnitName = hwUnitName.value,
            switchHwUnitName = switchHwUnitName.value,
            firebaseNotify = firebaseNotify.value,
            firebaseNotifyTrigger = firebaseNotifyTrigger.value,
            showInTaskList = showInTaskList.value,
            value = homeUnit?.value?.value,
            lastUpdateTime = homeUnit?.value?.lastUpdateTime,
            lastTriggerSource = homeUnit?.value?.lastTriggerSource,
            unitsTasks = unitTaskList.value.toMutableMap().also {
                it.remove("")
            })
    }

}
