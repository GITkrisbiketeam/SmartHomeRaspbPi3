package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.StorageUnitsListLiveData
import com.krisbiketeam.data.storage.dto.Room
import com.krisbiketeam.data.storage.dto.StorageUnit

/**
 * The ViewModel used in [RoomDetailFragment].
 */
class RoomDetailViewModel(
        homeRepository: HomeInformationRepository,
        roomName: String
) : ViewModel() {

    var isEditMode: MutableLiveData<Boolean> = MutableLiveData()
    val room: LiveData<Room>
    val storageUnits = MediatorLiveData<List<StorageUnit<out Any>>>()

    init {
        room = homeRepository.roomLiveData(roomName)
        storageUnits.addSource(homeRepository.storageUnitsLiveData(roomName), storageUnits::setValue)
        isEditMode.value = false
    }
}
