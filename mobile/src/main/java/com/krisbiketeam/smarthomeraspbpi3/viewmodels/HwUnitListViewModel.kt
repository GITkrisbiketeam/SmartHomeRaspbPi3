package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragment

/**
 * The ViewModel for [HwUnitListFragment].
 */
class HwUnitListViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {
    val hwUnitList: LiveData<List<HwUnit>> = homeRepository.hwUnitListFlow().asLiveData()

    val hwUnitListAdapter: HwUnitListAdapter = HwUnitListAdapter()

    fun restartAllHwUnits() {
        hwUnitList.value?.map { HwUnitLog<Any>(it) }?.let { hwUnitList ->
            homeRepository.addHwUnitListToRestart(hwUnitList)
        }
    }
}
