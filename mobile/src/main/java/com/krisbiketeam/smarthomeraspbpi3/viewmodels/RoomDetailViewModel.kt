package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.StorageUnitsLiveData
import com.krisbiketeam.data.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class RoomDetailViewModel(
        homeRepository: HomeInformationRepository,
        roomName: String
) : ViewModel() {

    var isEditMode: MutableLiveData<Boolean> = MutableLiveData()
    val room: LiveData<Room>
    val storageUnits: StorageUnitsLiveData


    init {
        room = homeRepository.roomLiveData(roomName)
        storageUnits = homeRepository.storageUnitsLiveData(roomName)
        isEditMode.value = false
    }
}
