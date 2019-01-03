package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import timber.log.Timber

private const val NO_GROW_ZONE = -1

/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(homeRepository: HomeInformationRepository) : ViewModel() {

    private val growZoneNumber = MutableLiveData<Int>()

    //TODO: why MediatorLiveData ??? there is only one LiveData attached to it
    private val roomList = MediatorLiveData<List<Room>>()

    init {
        Timber.d("init")

        growZoneNumber.value = NO_GROW_ZONE

        val livePlantList = Transformations.switchMap(growZoneNumber) {
            homeRepository.roomsLiveData()
        }
        roomList.addSource(livePlantList) {
            roomList.setValue(it) }
    }

    fun getRooms() = roomList

    fun setGrowZoneNumber(num: Int) {
        growZoneNumber.value = num
    }

    fun clearGrowZoneNumber() {
        growZoneNumber.value = NO_GROW_ZONE
    }

    fun isFiltered() = growZoneNumber.value != NO_GROW_ZONE
}
