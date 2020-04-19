package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragment
import timber.log.Timber

/**
 * The ViewModel for [HwUnitListFragment].
 */
class HwUnitListViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {
    val hwUnitList: LiveData<List<HwUnit>>

    val hwUnitListAdapter: HwUnitListAdapter = HwUnitListAdapter()

    init {
        Timber.d("init")

        hwUnitList = homeRepository.hwUnitListLiveData()
    }

    fun restartAllHwUnits() {
        hwUnitList.value?.map { HwUnitLog<Any>(it) }?.let { hwUnitList ->
            homeRepository.addHwUnitListToRestart(hwUnitList)
        }
    }
}
