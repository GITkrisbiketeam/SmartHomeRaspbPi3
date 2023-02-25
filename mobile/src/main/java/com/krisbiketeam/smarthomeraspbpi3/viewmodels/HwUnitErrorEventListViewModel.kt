package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitErrorEventListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorEventListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

/**
 * The ViewModel for [HwUnitErrorEventListFragment].
 */
class HwUnitErrorEventListViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    @ExperimentalCoroutinesApi
    val hwUnitErrorEventList: Flow<List<HwUnitLog<Any>>> = homeRepository.hwUnitErrorEventListFlow()

    val hwUnitErrorEventListAdapter: HwUnitErrorEventListAdapter = HwUnitErrorEventListAdapter()

    fun clearHwErrors(){
        homeRepository.clearHwErrors()
    }
}
