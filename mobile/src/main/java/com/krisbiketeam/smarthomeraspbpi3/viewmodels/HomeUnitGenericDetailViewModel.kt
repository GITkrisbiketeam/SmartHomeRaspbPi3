package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_ACTION_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitGenericDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The Base ViewModel used in [HomeUnitGenericDetailFragment].
 */
@ExperimentalCoroutinesApi
class HomeUnitGenericDetailViewModel(
    application: Application,
    homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, unitName: String?, unitType: HomeUnitType
) : HomeUnitDetailViewModelBase<GenericHomeUnit<Any>>(
    application,
    homeRepository,
    roomName,
    unitName,
    unitType
) {

    val minValue: MutableStateFlow<String> = MutableStateFlow("")
    val minLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val maxValue: MutableStateFlow<String> = MutableStateFlow("")
    val maxLastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val valueSwitchVisible: StateFlow<Boolean> =
        combine(isEditMode, type) { isEditMode, type ->
            !isEditMode && HOME_ACTION_STORAGE_UNITS.contains(type)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    override fun getHomeUnitFlow(unitType: HomeUnitType, unitName: String) =
        homeRepository.genericHomeUnitFlow(unitType, unitName)

    override fun initializeAdditionalHomeUnitStates(homeUnit: GenericHomeUnit<Any>) {
        minValue.value = homeUnit.min.toString()
        minLastUpdateTime.value = getLastUpdateTime(getApplication(), homeUnit.minLastUpdateTime)
        maxValue.value = homeUnit.max.toString()
        maxLastUpdateTime.value = getLastUpdateTime(getApplication(), homeUnit.maxLastUpdateTime)
    }

    fun clearMinValue(): Task<Void>? {
        Timber.d("clearMinValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let(homeRepository::clearMinHomeUnitValue)
    }

    fun clearMaxValue(): Task<Void>? {
        Timber.d("clearMaxValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let(homeRepository::clearMaxHomeUnitValue)
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

    override fun additionalNoChangesMade(homeUnit: GenericHomeUnit<Any>): Boolean {
        return true
    }

    override fun restoreAdditionalHomeUnitInitialStates(homeUnit: GenericHomeUnit<Any>) {
        // do Nothing
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    override fun actionSaveGetCustomSavePair(): Pair<Int, Int?>? {
        return null
    }

    override fun getHomeUnitToSave(): GenericHomeUnit<Any> {
        return GenericHomeUnit(
            name = name.value,
            type = type.value,
            room = room.value,
            hwUnitName = hwUnitName.value,
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
