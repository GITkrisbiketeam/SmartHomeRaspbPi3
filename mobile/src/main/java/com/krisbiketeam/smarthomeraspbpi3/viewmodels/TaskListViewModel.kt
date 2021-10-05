package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.model.TaskListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.mapNotNull
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toMutableList

/**
 * The ViewModel for [TaskListFragment].
 */
@ExperimentalCoroutinesApi
class TaskListViewModel(private val homeRepository: FirebaseHomeInformationRepository, secureStorage: SecureStorage) : ViewModel() {

    val isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var localItemsOrder: List<String> = listOf()

    val taskListFromFlow: Flow<List<TaskListAdapterModel>> = secureStorage.homeNameFlow.flatMapLatest {
        Timber.e("secureStorage.homeNameLiveData")
        combine(homeRepository.homeUnitListFlow().debounce(100), homeRepository.hwUnitErrorEventListFlow(), homeRepository.taskListOrderFlow()) { homeUnitsList, hwUnitErrorEventList, itemsOrder ->
            Timber.e("taskListAdapterModelMap")
            val taskListAdapterModelMap: MutableMap<String, TaskListAdapterModel> = mutableMapOf()
            // Add HomeUnits without Room set
            homeUnitsList.filter {
                it.room.isEmpty() || it.hwUnitName.isNullOrEmpty() || it.showInTaskList
            }.forEach {
                taskListAdapterModelMap[it.type + '.' + it.name] = TaskListAdapterModel(null, it, hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null)
            }

            // save current RoomListOrder
            localItemsOrder = itemsOrder

            // return sorted list
            mutableListOf<TaskListAdapterModel>().apply {
                itemsOrder.forEach {
                    taskListAdapterModelMap.remove(it)?.run(this::add)
                }
                // add leftOvers
                addAll(taskListAdapterModelMap.values)
                // save new updated order
                apply {
                    val newItemsOrder = this.mapNotNull { model ->
                        model.room?.name ?: model.homeUnit?.let { it.type + '.' + it.name }
                    }
                    if (newItemsOrder != localItemsOrder) {
                        localItemsOrder = newItemsOrder
                        homeRepository.saveTaskListOrder(newItemsOrder)
                    }
                }
            }
        }
    }

    fun moveItem(from: Int, to: Int) {
        val itemsOrderCopy = localItemsOrder.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)
        homeRepository.saveTaskListOrder(itemsOrderCopy)
    }
}

