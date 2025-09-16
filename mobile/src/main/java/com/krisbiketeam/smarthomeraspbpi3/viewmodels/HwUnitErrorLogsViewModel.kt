package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitErrorLogsListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.ThingsAppLogsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The ViewModel for [ThingsAppLogsFragment].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HwUnitErrorLogsViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
) : ViewModel() {

    val hwUnitErrorLogsListAdapter: HwUnitErrorLogsListAdapter = HwUnitErrorLogsListAdapter()

    // List of HwUnits with their value name ex. temperature or humidity
    private val filteredHwUnitListFlow: MutableStateFlow<List<HwUnit>> =
        MutableStateFlow(emptyList())

    val menuItemHwUnitListFlow: StateFlow<List<Triple<HwUnit, Int, Boolean>>> =
        combine(
            filteredHwUnitListFlow,
            homeRepository.hwUnitListFlow()
        ) { filteredHwUnitList, hwUnitList ->
            Timber.e("combine")
            hwUnitList.map { hwUnit ->
                Triple(
                    hwUnit,
                    hwUnit.hashCode(),
                    filteredHwUnitList.contains(hwUnit)
                )
            }.sortedByDescending {
                it.first.type
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    @ExperimentalCoroutinesApi
    val logsData: Flow<List<HwUnitLog<Any>>> =
        filteredHwUnitListFlow.flatMapLatest { selectedItems ->
            combine(selectedItems.map { hwUnit ->
                homeRepository.hwUnitErrorLogsFlow(hwUnit.name).onCompletion {
                    Timber.e("onCompletion ${hwUnit.name}")
                    emit(listOf())
                }.onStart {
                    Timber.e("onStart ${hwUnit.name}")
                    emit(listOf())
                }
            }) { hwUnitErrorLogArray ->
                val combinedList: MutableList<HwUnitLog<Any>> = mutableListOf()
                hwUnitErrorLogArray.forEach {
                    combinedList.addAll(it)
                }
                combinedList.sortedByDescending { it.localtime }
            }
        }.flowOn(Dispatchers.IO)

    fun clearLogs() {
        if (filteredHwUnitListFlow.value.isEmpty()) {
            homeRepository.clearAllHwUnitErrorLogs()
        } else {
            filteredHwUnitListFlow.value.forEach {
                homeRepository.clearHwUnitErrorLogs(it.name)
            }
        }
    }

    fun isSelectAll(): Boolean = menuItemHwUnitListFlow.value.firstOrNull {
        !it.third
    } == null

    @ExperimentalCoroutinesApi
    fun selectAll(isChecked: Boolean) {
        filteredHwUnitListFlow.update {
            if (isChecked) {
                menuItemHwUnitListFlow.value.map {
                    it.first
                }
            } else {
                emptyList()
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun addFilter(hwUnitHash: Int): Boolean {
        return menuItemHwUnitListFlow.value.firstOrNull { it.second == hwUnitHash }
            ?.let { (hwUnit, _, checked) ->
                val newList = filteredHwUnitListFlow.value.toMutableList()
                if (checked) {
                    newList.remove(hwUnit)
                } else {
                    newList.add(hwUnit)
                }
                filteredHwUnitListFlow.value = newList
                true
            } ?: false
    }
}

