package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * The ViewModel for [HwUnitListFragment].
 */
@ExperimentalCoroutinesApi
class HwUnitListViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {
    val hwUnitList: StateFlow<List<HwUnit>> = homeRepository.hwUnitListFlow().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val hwUnitListAdapter: HwUnitListAdapter = HwUnitListAdapter()

    fun restartAllHwUnits() {
        hwUnitList.value.map { HwUnitLog<Any>(it) }.let { hwUnitList ->
            homeRepository.addHwUnitListToRestart(hwUnitList)
        }
    }
}
