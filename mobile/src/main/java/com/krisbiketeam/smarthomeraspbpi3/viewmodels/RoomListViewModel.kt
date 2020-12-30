package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_TEMPERATURES
import com.krisbiketeam.smarthomeraspbpi3.model.RoomListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import timber.log.Timber

/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(private val homeRepository: FirebaseHomeInformationRepository, secureStorage: SecureStorage) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)

    private var localItemsOrder: List<String> = listOf()

    @ExperimentalCoroutinesApi
    val roomWithHomeUnitsListFromFlow: LiveData<List<RoomListAdapterModel>> = secureStorage.homeNameLiveData.switchMap {
        Timber.e("secureStorage.homeNameLiveData")
        combine(homeRepository.roomListFlow(), homeRepository.homeUnitListFlow().debounce(100), homeRepository.hwUnitErrorEventListFlow(), homeRepository.roomListOrderFlow()) { roomList, homeUnitsList, hwUnitErrorEventList, itemsOrder ->
            Timber.e("roomListAdapterModelMap")
            val roomListAdapterModelMap: MutableMap<String, RoomListAdapterModel> = roomList.associate {
                it.name to RoomListAdapterModel(it)
            }.toMutableMap()

            val homeUnitsListCopy = homeUnitsList.toMutableList()
            homeUnitsList.forEach {
                if (roomListAdapterModelMap.containsKey(it.room)) {
                    // set Given room Temperature if present
                    if (it.type == HOME_TEMPERATURES) {
                        roomListAdapterModelMap[it.room]?.homeUnit = it
                    }
                    roomListAdapterModelMap[it.room]?.error =
                            roomListAdapterModelMap[it.room]?.error == true || hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null
                    homeUnitsListCopy.remove(it)
                }
            }
            // Add HomeUnits without Room set
            homeUnitsListCopy.forEach {
                roomListAdapterModelMap[it.name] = RoomListAdapterModel(null, it, hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null)
            }

            // save current RoomListOrder
            localItemsOrder = itemsOrder

            // return sorted list
            mutableListOf<RoomListAdapterModel>().apply {
                itemsOrder.forEach {
                    roomListAdapterModelMap.remove(it)?.run(this::add)
                }
                // add leftOvers
                addAll(roomListAdapterModelMap.values)
                // save new updated order
                apply {
                    val newItemsOrder = this.mapNotNull {
                        it.room?.name?:it.homeUnit?.name
                    }
                    if (newItemsOrder != localItemsOrder) {
                        localItemsOrder = newItemsOrder
                        homeRepository.saveRoomListOrder(newItemsOrder)
                    }
                }
            }
        }.asLiveData(Dispatchers.Default)
    }

    fun moveItem(from: Int, to: Int) {
        val itemsOrderCopy = localItemsOrder.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)
        homeRepository.saveRoomListOrder(itemsOrderCopy)
    }
}

