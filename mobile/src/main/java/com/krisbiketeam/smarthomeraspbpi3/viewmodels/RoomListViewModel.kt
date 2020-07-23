package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.model.RoomListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import timber.log.Timber

private const val UNIT_LIST_SECTION_EXIST = "unit_list_section_exist"

/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(homeRepository: FirebaseHomeInformationRepository, secureStorage: SecureStorage) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)

    val roomHomeUnitsMap = Transformations.switchMap(secureStorage.homeNameLiveData) {
        MediatorLiveData<MutableMap<String, Any>>().apply {
            // TODO: add background coroutine
            HOME_STORAGE_UNITS.forEach { type ->
                addSource(homeRepository.homeUnitListLiveData(type)) { homeUnitList ->
                    //TODO: we should use copy on LiveData value not modify it constantly as it will trigger update
                    value = value ?: mutableMapOf()

                    val homeUnitListToRemove = mutableListOf<String>()
                    value?.keys?.forEach {
                        value?.get(it)?.let { item ->
                            if (item is HomeUnit<*> && item.type == type) {
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
                    if (value?.get(it) is Room?) {
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

    @ExperimentalCoroutinesApi
    val roomWithHomeUnitsListFromFlow: LiveData<List<RoomListAdapterModel>> = secureStorage.homeNameLiveData.switchMap {
        Timber.e("secureStorage.homeNameLiveData")
        combine(homeRepository.roomListFlow(), homeRepository.homeUnitListFlow()) { roomList, homeUnitsList ->
            Timber.e("roomListAdapterModelMap")
            val roomListAdapterModelMap: MutableMap<String, RoomListAdapterModel> = roomList.associate {
                it.name to RoomListAdapterModel(it)
            }.toMutableMap()

            val homeUnitsListCopy = homeUnitsList.toMutableList()
            homeUnitsList.forEach {
                if (roomListAdapterModelMap.containsKey(it.room)) {
                    roomListAdapterModelMap[it.room]?.homeUnit = it
                    homeUnitsListCopy.remove(it)
                }
            }
            homeUnitsListCopy.forEach {
                roomListAdapterModelMap[it.name] = RoomListAdapterModel(null, it)
            }
            roomListAdapterModelMap.values.toList()
        }.asLiveData(Dispatchers.Default)
    }
}

