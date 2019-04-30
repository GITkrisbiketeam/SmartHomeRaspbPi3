package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment

private const val UNIT_LIST_SECTION_EXIST = "unit_list_section_exist"
/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(homeRepository: HomeInformationRepository) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)
    val roomHomeUnitsMap = MediatorLiveData<MutableMap<String, Any>>().apply {
        HOME_STORAGE_UNITS.forEach { type ->
            addSource(homeRepository.homeUnitListLiveData(type)) { homeUnitList ->
                value = value ?: mutableMapOf()

                val homeUnitListToRemove = mutableListOf<String>()
                value?.keys?.forEach {
                    value?.get(it)?.let {item ->
                        if (item is HomeUnit<*> && item.type == type){
                            homeUnitListToRemove.add(it)
                            homeUnitListToRemove.add(UNIT_LIST_SECTION_EXIST)
                        }
                    }
                }
                homeUnitListToRemove.forEach {
                    value?.remove(it)
                }

                homeUnitList.filter {
                    it.room.isEmpty()
                }.forEach {
                    value?.put(UNIT_LIST_SECTION_EXIST, "Units without Room:")
                    value?.put(it.name, it)
                }
                postValue(value)
            }
        }
        addSource(homeRepository.roomListLiveData()) { roomList ->
            value = value ?: mutableMapOf()
            val roomListToRemove = mutableListOf<String>()
            value?.keys?.forEach {
                if (value?.get(it) is Room?){
                    roomListToRemove.add(it)
                }
            }
            roomListToRemove.forEach {
                value?.remove(it)
            }
            roomList.forEach {
                value?.put(it.name, it)
            }
            postValue(value)
        }
    }
}
