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
import timber.log.Timber

/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(homeRepository: FirebaseHomeInformationRepository, secureStorage: SecureStorage) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)

    @ExperimentalCoroutinesApi
    val roomWithHomeUnitsListFromFlow: LiveData<List<RoomListAdapterModel>> = secureStorage.homeNameLiveData.switchMap {
        Timber.e("secureStorage.homeNameLiveData")
        combine(homeRepository.roomListFlow(), homeRepository.homeUnitListFlow(), homeRepository.hwUnitErrorEventListFlow()) { roomList, homeUnitsList, hwUnitErrorEventList ->
            Timber.e("roomListAdapterModelMap")
            val roomListAdapterModelMap: MutableMap<String, RoomListAdapterModel> = roomList.associate {
                it.name to RoomListAdapterModel(it)
            }.toMutableMap()

            val homeUnitsListCopy = homeUnitsList.toMutableList()
            homeUnitsList.forEach {
                if (roomListAdapterModelMap.containsKey(it.room)) {
                    if (it.type == HOME_TEMPERATURES) {
                        roomListAdapterModelMap[it.room]?.homeUnit = it
                    }
                    roomListAdapterModelMap[it.room]?.error =
                            roomListAdapterModelMap[it.room]?.error == true || hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null
                    homeUnitsListCopy.remove(it)
                }
            }
            homeUnitsListCopy.forEach {
                roomListAdapterModelMap[it.name] = RoomListAdapterModel(null, it, hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null)
            }
            roomListAdapterModelMap.values.toList()
        }.asLiveData(Dispatchers.Default)
    }
}

