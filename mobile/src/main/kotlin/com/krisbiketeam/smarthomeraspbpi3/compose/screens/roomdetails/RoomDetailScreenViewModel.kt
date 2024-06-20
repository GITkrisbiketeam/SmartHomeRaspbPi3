package com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomdetails

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartAlertDialogModel
import com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit.HomeUnitCardModel
import com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit.HomeUnitCardModelId
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitGenericDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.collections.set


/**
 * The ViewModel used in [RoomDetailFragment].
 */
@ExperimentalCoroutinesApi
@FlowPreview
class RoomDetailScreenViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
    private val inputRoomName: String
) : ViewModel() {

    private val roomList = homeRepository.roomListFlow().flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val homeUnitsOrderStateFlow = MutableStateFlow<List<String>>(emptyList())
    private val roomName: MutableStateFlow<String> = MutableStateFlow(inputRoomName)
    private var editRoomName: String = inputRoomName

    private val room: StateFlow<Room?> =
        roomName.flatMapLatest { homeRepository.roomUnitFlow(it).flowOn(Dispatchers.IO) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val showProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val showDialog: MutableStateFlow<SmartAlertDialogModel?> = MutableStateFlow(null)

    val navigateUp: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val homeUnitsList: StateFlow<List<HomeUnitCardModel>> by lazy {
        combine(
            homeRepository.homeUnitListFlow().debounce(100),
            homeRepository.hwUnitErrorEventListFlow(),
            homeUnitsOrderStateFlow,
            room.map {
                it?.unitsOrder ?: emptyList()
            }) { homeUnitList, hwUnitErrorEventList, newOrderList, existingOrderList ->
            Timber.i("homeUnitsMap Flow")
            val orderList = newOrderList.ifEmpty { existingOrderList }
            val map: MutableMap<String, HomeUnitCardModel?> =
                orderList.associateWithTo(LinkedHashMap(orderList.size)) { null }
            homeUnitList.forEach {
                if (it.room == room.value?.name) {
                    //Timber.i("homeUnitsMap Flow filter")
                    val isError =
                        hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null
                    map[it.type.toString() + '.' + it.name] = when (it.type) {
                        HomeUnitType.HOME_TEMPERATURES,
                        HomeUnitType.HOME_PRESSURES,
                        HomeUnitType.HOME_HUMIDITY,
                        HomeUnitType.HOME_GAS,
                        HomeUnitType.HOME_GAS_PERCENT,
                        HomeUnitType.HOME_IAQ,
                        HomeUnitType.HOME_STATIC_IAQ,
                        HomeUnitType.HOME_CO2,
                        HomeUnitType.HOME_BREATH_VOC -> {
                            it.value.let { value ->
                                if (value is Number?) {
                                    HomeUnitCardModel.FloatHomeUnitCardModel(
                                        HomeUnitCardModelId(it.type, it.name, it.hwUnitName),
                                        it.name,
                                        value,
                                        it.lastUpdateTime,
                                        isError
                                    )
                                } else {
                                    null
                                }
                            }
                        }

                        HomeUnitType.HOME_REED_SWITCHES,
                        HomeUnitType.HOME_MOTIONS -> {
                            it.value.let { value ->
                                if (value is Boolean?) {
                                    HomeUnitCardModel.BooleanHomeUnitCardModel(
                                        HomeUnitCardModelId(it.type, it.name, it.hwUnitName),
                                        it.name,
                                        value,
                                        it.lastUpdateTime,
                                        isError
                                    )
                                } else {
                                    null
                                }
                            }
                        }

                        HomeUnitType.HOME_ACTUATORS -> {
                            it.value.let { value ->
                                if (value is Boolean?) {
                                    HomeUnitCardModel.SwitchHomeUnitCardModel(
                                        HomeUnitCardModelId(it.type, it.name, it.hwUnitName),
                                        it.name,
                                        value,
                                        it.lastUpdateTime,
                                        isError
                                    )
                                } else {
                                    null
                                }
                            }
                        }

                        HomeUnitType.HOME_LIGHT_SWITCHES -> {
                            if (it is LightSwitchHomeUnit<*>) {
                                val value = it.value
                                val switchValue = it.switchValue
                                if (value is Boolean? && switchValue is Boolean?) {
                                    HomeUnitCardModel.LightSwitchHomeUnitCardModel(
                                        HomeUnitCardModelId(it.type, it.name, it.hwUnitName),
                                        it.name,
                                        value,
                                        it.lastUpdateTime,
                                        switchValue,
                                        it.switchLastUpdateTime,
                                        isError
                                    )
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }

                        HomeUnitType.UNKNOWN,
                        HomeUnitType.HOME_BLINDS,
                        HomeUnitType.HOME_WATER_CIRCULATION,
                        HomeUnitType.HOME_MCP23017_WATCH_DOG -> null
                    }
                }
            }
            map.values.filterNotNull().also { unitsList ->
                val newOrder =
                    unitsList.map { it.id.let { homeUnit -> homeUnit.homeUnitType.toString() + '.' + homeUnit.homeUnitName } }
                if (newOrder != orderList || newOrderList.isEmpty()) {
                    homeUnitsOrderStateFlow.value = newOrder
                }
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    fun switchHomeUnitState(homeUnit: Pair<HomeUnitType, String>, switchState: Boolean) {
        homeRepository.updateHomeUnitValue(
            homeUnit.first,
            homeUnit.second,
            switchState,
            System.currentTimeMillis(),
            LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
        )
    }

    fun setRoomName(name: String) {
        editRoomName = name
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    @StringRes
    fun actionSave(): Int? {
        Timber.d("actionSave room.name: ${room.value?.name} roomName.value: ${roomName.value}")
        return when {
            editRoomName.isEmpty() -> R.string.new_room_empty_name
            noChangesMade() -> R.string.add_edit_home_unit_no_changes
            roomList.value.find { room -> room.name == roomName.value.trim() } != null -> R.string.new_room_name_already_used

            else -> {
                showDialog.value = SmartAlertDialogModel(
                    R.string.save_room,
                    R.string.add_edit_home_unit_overwrite_changes,
                    R.string.overwrite
                ) {
                    viewModelScope.launch(Dispatchers.IO) {
                        saveChanges()
                    }
                }
                null
            }
        }
    }

    /**
     * return true if we want to exit [HomeUnitGenericDetailFragment]
     */
    fun actionDiscard(): Boolean {
        return if (!noChangesMade()) {
            showDialog.value = SmartAlertDialogModel(
                R.string.save_room,
                R.string.add_edit_home_unit_discard_changes,
                R.string.menu_discard
            ) {
                roomName.value = room.value?.name ?: inputRoomName
                homeUnitsOrderStateFlow.value = room.value?.unitsOrder ?: emptyList()
            }
            false
        } else {
            true
        }
    }

    fun actionDeleteRoom() {
        showDialog.value = SmartAlertDialogModel(
            R.string.delete_room,
            R.string.add_edit_home_unit_delete_home_unit_prompt,
            R.string.menu_delete
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                deleteRoom()
            }
        }
    }


    fun actionMoveItem(from: Int, to: Int) {
        val itemsOrderCopy = homeUnitsOrderStateFlow.value.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)

        homeUnitsOrderStateFlow.value = itemsOrderCopy
    }


    private fun noChangesMade(): Boolean {
        return editRoomName == room.value?.name
                && room.value?.name == roomName.value.trim()
                && room.value?.unitsOrder == homeUnitsOrderStateFlow.value
    }

    private fun saveChanges() {
        showProgress.value = true
        roomName.value = editRoomName
        roomName.value.let { newRoomName ->
            homeUnitsList.value.let { homeUnitList ->
                Tasks.whenAll(if (newRoomName != room.value?.name) {
                    homeUnitList.map { homeUnitModel ->
                        homeRepository.updateHomeUnitRoomName(
                            homeUnitModel.id.homeUnitType,
                            homeUnitModel.id.homeUnitName,
                            newRoomName
                        )
                    }
                } else null).continueWithTask {
                    room.value?.let { room ->
                        val oldRoomName = room.name
                        homeRepository.saveRoom(room.apply {
                            name = newRoomName
                            unitsOrder = homeUnitsOrderStateFlow.value
                        })?.continueWithTask { saveRoomTask ->
                            if (newRoomName != oldRoomName) {
                                homeRepository.deleteRoom(oldRoomName) ?: saveRoomTask
                            } else {
                                Tasks.forResult(null)
                            }
                        } ?: it
                    } ?: it
                }.addOnCompleteListener {
                    Timber.d("Task completed")
                    showProgress.value = false
                    navigateUp.value = true
                }
            }
        }
    }

    private fun deleteRoom(): Task<Void> {
        Timber.d("deleteHomeUnit room.name: ${room.value?.name} ")
        showProgress.value = true
        return homeUnitsList.value.let { homeUnitList ->
            Tasks.whenAll(homeUnitList.map { homeUnitModel ->
                homeRepository.updateHomeUnitRoomName(
                    homeUnitModel.id.homeUnitType,
                    homeUnitModel.id.homeUnitName,
                    ""
                )
            }).continueWithTask {
                room.value?.let { room ->
                    homeRepository.deleteRoom(room.name)
                } ?: it
            }.addOnCompleteListener {
                Timber.d("Task completed")
                showProgress.value = false
                navigateUp.value = true
            }
        }
    }
}
