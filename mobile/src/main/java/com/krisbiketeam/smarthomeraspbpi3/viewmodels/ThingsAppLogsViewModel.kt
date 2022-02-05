package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.adapters.ThingsAppLogsListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyDateLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog
import com.krisbiketeam.smarthomeraspbpi3.ui.ThingsAppLogsFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The ViewModel for [ThingsAppLogsFragment].
 */
class ThingsAppLogsViewModel(private val homeRepository: FirebaseHomeInformationRepository, private val secureStorage: SecureStorage) : ViewModel() {

    val thingsAppLogsListAdapter: ThingsAppLogsListAdapter = ThingsAppLogsListAdapter()

    @ExperimentalCoroutinesApi
    val startRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    @ExperimentalCoroutinesApi
    val endRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    @ExperimentalCoroutinesApi
    val logsData: Flow<List<RemoteLog>> =
            combine(startRangeFlow, endRangeFlow) { startRange, endRange ->
                Pair(startRange, endRange)
            }.flatMapLatest { (startRange, endRange) ->
                val flowList = mutableListOf<Flow<Map<String, RemoteLog>>>().also { list ->
                    // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
                    for (date in startRange..endRange step FULL_DAY_IN_MILLIS) {
                        list.add(homeRepository.thingsAppLogsFlow(date).onCompletion {
                            Timber.e("onCompletion")
                            emit(mapOf())
                        }.onStart {
                            Timber.e("onStart")
                            emit(mapOf())
                        })
                    }
                }
                combine(flowList) { dailyMapArray ->
                    val combinedMap: MutableMap<String, RemoteLog> = mutableMapOf()
                    dailyMapArray.forEach {
                        combinedMap.putAll(it)
                    }
                    combinedMap.toSortedMap().values.reversed()
                }
            }

    @ExperimentalCoroutinesApi
    val menuItemRemoteLogListFlow: StateFlow<List<Triple<String, Int, Boolean>>> =
            secureStorage.remoteLoggingLevelFlow.map { level ->
                listOf(
                        Triple("VERBOSE", Log.VERBOSE, level == Log.VERBOSE),
                        Triple("DEBUG", Log.DEBUG, level == Log.DEBUG),
                        Triple("INFO", Log.INFO, level == Log.INFO),
                        Triple("WARN", Log.WARN, level == Log.WARN),
                        Triple("ERROR", Log.ERROR, level == Log.ERROR),
                        Triple("OFF", Int.MAX_VALUE, level > Log.ERROR || level < Log.VERBOSE)
                )
            }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    emptyList()
            )


    fun clearLogs() = homeRepository.clearAllThingsLog()

    fun setLogLevel(level: Int): Boolean {
        return if ((level >= Log.VERBOSE && level <= Log.ERROR) || level == Int.MAX_VALUE) {
            secureStorage.remoteLoggingLevel = level
            true
        } else {
            false
        }
    }
}

