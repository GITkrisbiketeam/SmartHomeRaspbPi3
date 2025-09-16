package com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomlist

import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
import com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard.CardColorState
import com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard.SmartUnitCardModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce

/**
 * The ViewModel for [RoomListScreen].
 */
class RoomListScreenViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
) : ViewModel() {

    private var localItemsOrder: List<String> = listOf()

    val roomListFlow: Flow<List<SmartUnitCardModel>> = combine(
        homeRepository.roomListFlow(),
        homeRepository.homeUnitListFlow().debounce(100),
        homeRepository.hwUnitErrorEventListFlow(),
        homeRepository.roomListOrderFlow()
    ) { roomList, homeUnitsList, hwUnitErrorEventList, itemsOrder ->
        val roomListModelMap: MutableMap<String, SmartUnitCardModel> = roomList.associate {
            it.name to SmartUnitCardModel(it.name)
        }.toMutableMap()

        homeUnitsList.forEach { homeUnit ->
            roomListModelMap.computeIfPresent(homeUnit.room) { _, model ->
                var updatedModel = model
                if (homeUnit.type == HomeUnitType.HOME_TEMPERATURES) {
                    val value: String = homeUnit.value?.let {
                        if (it is Number) {
                            String.format("%.1f", it)
                        } else {
                            "null"
                        }
                    } ?: "null"

                    updatedModel = model.copy(subtitle = value)
                }
                if (homeUnit.type == HomeUnitType.HOME_LIGHT_SWITCHES && homeUnit is LightSwitchHomeUnit) {
                    homeUnit.value.let { switchValue ->
                        if (switchValue is Boolean?) {
                            updatedModel = model.copy(
                                switchState = switchValue ?: false,
                                switchText = "Light",
                                switchUnit = homeUnit.type to homeUnit.name
                            )
                        }
                    }

                }
                if (homeUnit.type == HomeUnitType.HOME_MOTIONS && homeUnit is GenericHomeUnit) {
                    homeUnit.value.let { motionValue ->
                        if (motionValue == true) {
                            updatedModel = model.copy(
                                background =  CardColorState.MOTION
                            )
                        }
                    }

                }
                if (homeUnit.type == HomeUnitType.HOME_REED_SWITCHES && homeUnit is GenericHomeUnit) {
                    homeUnit.value.let { reedValue ->
                        if (reedValue == true) {
                            updatedModel = model.copy(
                                background =  CardColorState.REED_SWITCH
                            )
                        }
                    }

                }
                if (hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == homeUnit.hwUnitName } != null) {
                    updatedModel = model.copy(background =  CardColorState.ERROR)
                }
                updatedModel
            }
        }

        // save current RoomListOrder
        localItemsOrder = itemsOrder

        // return sorted list
        mutableListOf<SmartUnitCardModel>().apply {
            itemsOrder.forEach {
                roomListModelMap.remove(it)?.run(this::add)
            }
            // add leftOvers
            addAll(roomListModelMap.values)
            // save new updated order
            apply {
                val newItemsOrder = this.map { model ->
                    model.title
                }
                if (newItemsOrder != localItemsOrder) {
                    localItemsOrder = newItemsOrder
                    homeRepository.saveRoomListOrder(newItemsOrder)
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
            LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
        )
    }

    fun moveItem(from: Int, to: Int) {
        val itemsOrderCopy = localItemsOrder.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)
        homeRepository.saveRoomListOrder(itemsOrderCopy)
    }
}

