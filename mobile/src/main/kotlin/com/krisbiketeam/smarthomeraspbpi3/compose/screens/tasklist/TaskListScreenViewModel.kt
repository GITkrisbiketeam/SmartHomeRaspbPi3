package com.krisbiketeam.smarthomeraspbpi3.compose.screens.tasklist

import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.MCP23017WatchDogHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.WaterCirculationHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_TASK_LIST
import com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard.SmartUnitCardModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import timber.log.Timber

/**
 * The ViewModel for [TaskListScreen].
 */
class TaskListScreenViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
) : ViewModel() {

    private var localItemsOrder: List<String> = listOf()

    val roomListFlow: Flow<List<SmartUnitCardModel>> = combine(
        homeRepository.homeUnitListFlow().debounce(100),
        homeRepository.hwUnitErrorEventListFlow(),
        homeRepository.taskListOrderFlow()
    ) { homeUnitsList, hwUnitErrorEventList, itemsOrder ->

        Timber.e("roomListAdapterModelMap")
        val taskListModelMap: MutableMap<String, SmartUnitCardModel> = mutableMapOf()

        homeUnitsList.filter {
            it.room.isEmpty() || it.hwUnitName.isNullOrEmpty() || it.showInTaskList
        }.forEach { homeUnit ->

            val description: String? =
                if (homeUnit.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnit is WaterCirculationHomeUnit && homeUnit.temperatureValue is Float) {
                    String.format("%.2f", homeUnit.temperatureValue)
                } else {
                    null
                }

            val switchState: Boolean? = when (homeUnit.type) {
                HomeUnitType.HOME_LIGHT_SWITCHES,
                HomeUnitType.HOME_WATER_CIRCULATION,
                HomeUnitType.HOME_MCP23017_WATCH_DOG,
                HomeUnitType.HOME_ACTUATORS,
                HomeUnitType.HOME_BLINDS -> {
                    homeUnit.value.let { value -> if (value is Boolean?) value else null }
                }

                else -> null
            }

            val switchText =
                if (homeUnit.value is Number) {
                    String.format("%.2f", homeUnit.value)
                } else if (homeUnit.type == HomeUnitType.HOME_LIGHT_SWITCHES && homeUnit is LightSwitchHomeUnit) {
                    homeUnit.switchValue.toString()
                } else if (homeUnit.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnit is WaterCirculationHomeUnit) {
                    homeUnit.motionValue.toString()
                } else if (homeUnit.type == HomeUnitType.HOME_MCP23017_WATCH_DOG && homeUnit is MCP23017WatchDogHomeUnit) {
                    homeUnit.inputValue.toString()
                } else {
                    homeUnit.value.toString()
                }

            taskListModelMap[homeUnit.type.toString() + '.' + homeUnit.name] = SmartUnitCardModel(
                title = homeUnit.name,
                subtitle = description,
                switchState = switchState,
                switchText = switchText,
                switchUnit = homeUnit.type to homeUnit.name,
                error = hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == homeUnit.hwUnitName } != null)
        }

        // save current RoomListOrder
        localItemsOrder = itemsOrder

        // return sorted list
        mutableListOf<SmartUnitCardModel>().apply {
            itemsOrder.forEach {
                taskListModelMap.remove(it)?.run(this::add)
            }
            // add leftOvers
            addAll(taskListModelMap.values)
            // save new updated order
            apply {
                val newItemsOrder = this.mapNotNull { model ->
                    model.switchUnit?.let { (homeUnitType, homeUnitName) -> "$homeUnitType.$homeUnitName" }
                }
                if (newItemsOrder != localItemsOrder) {
                    localItemsOrder = newItemsOrder
                    homeRepository.saveTaskListOrder(newItemsOrder)
                }
            }
        }
    }

    fun switchHomeUnitState(type: HomeUnitType, name: String, switchState: Boolean) {
        homeRepository.updateHomeUnitValue(
            type,
            name,
            switchState,
            System.currentTimeMillis(),
            LAST_TRIGGER_SOURCE_TASK_LIST
        )
    }

    fun moveItem(from: Int, to: Int) {
        val itemsOrderCopy = localItemsOrder.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)
        homeRepository.saveTaskListOrder(itemsOrderCopy)
    }
}

