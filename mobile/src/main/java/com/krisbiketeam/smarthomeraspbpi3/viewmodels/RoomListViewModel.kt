package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import timber.log.Timber

private const val NO_GROW_ZONE = -1

/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel internal constructor(
    private val homeRepository: HomeInformationRepository
) : ViewModel() {

    private val growZoneNumber = MutableLiveData<Int>()

    //TODO: why MediatorLiveData ??? there is only one LiveData attached to it
    private val roomList = MediatorLiveData<List<Room>>()

    init {
        Timber.d("init")

        growZoneNumber.value = NO_GROW_ZONE

        val livePlantList = Transformations.switchMap(growZoneNumber) {
            homeRepository.roomsLiveData()
        }
        roomList.addSource(livePlantList, roomList::setValue)
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
