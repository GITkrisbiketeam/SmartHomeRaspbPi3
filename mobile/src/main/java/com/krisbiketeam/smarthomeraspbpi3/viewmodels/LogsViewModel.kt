package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.*
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyDateLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsFloat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * The ViewModel for [RoomListFragment].
 */
class LogsViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    private val colorFloatArray = FloatArray(3) { idx ->
        when (idx) {
            1 -> 1f
            2 -> 0.5f
            else -> 0f
        }
    }

    // List of HwUnits with their value name ex. temperature or humidity
    @ExperimentalCoroutinesApi
    private val filteredHwUnitListFlow: MutableStateFlow<List<Pair<HwUnit, String?>>> = MutableStateFlow(emptyList())

    @ExperimentalCoroutinesApi
    val startRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    @ExperimentalCoroutinesApi
    val endRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())


    @ExperimentalCoroutinesApi
    val menuItemHwUnitListFlow: StateFlow<List<Triple<Pair<HwUnit, String?>, Int, Boolean>>> =
            combine(filteredHwUnitListFlow, homeRepository.hwUnitListFlow()) { filteredHwUnitList, hwUnitList ->
                mutableListOf<Triple<Pair<HwUnit, String?>, Int, Boolean>>().apply {
                    hwUnitList.forEach { hwUnit ->
                        when (hwUnit.type) {
                            BoardConfig.TEMP_RH_SENSOR_SI7021,
                            BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                                listOf("temperature", "humidity").forEach {
                                    val pair = hwUnit to it
                                    add(Triple(pair, pair.hashCode(), filteredHwUnitList.contains(pair)))
                                }
                            }
                            BoardConfig.PRESS_TEMP_SENSOR_LPS331 -> {
                                listOf("pressure", "temperature").forEach {
                                    val pair = hwUnit to it
                                    add(Triple(pair, pair.hashCode(), filteredHwUnitList.contains(pair)))
                                }
                            }
                            BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                                listOf("temperature", "humidity", "pressure", "iaq", "gas", "staticIaq",
                                        "co2Equivalent", "breathVocEquivalent", "compGasValue",
                                        "gasPercentage").forEach {
                                    val pair = hwUnit to it
                                    add(Triple(pair, pair.hashCode(), filteredHwUnitList.contains(pair)))
                                }
                            }
                            else -> {
                                val pair = hwUnit to null
                                add(Triple(pair, pair.hashCode(), filteredHwUnitList.contains(pair)))
                            }
                        }
                    }
                }.sortedByDescending {
                    it.first.first.type
                }
            }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    emptyList()
            )

    @ExperimentalCoroutinesApi
    val logsData: Flow<ChartData<*>> =
            combine(startRangeFlow, endRangeFlow, filteredHwUnitListFlow) { startRange, endRange, filteredHwUnitList ->
                Triple(startRange, endRange, filteredHwUnitList)
            }.flatMapLatest { (startRange, endRange, filteredHwUnitList) ->
                if (filteredHwUnitList.isNullOrEmpty()) {
                    flowOf(CombinedData())
                } else {
                    val filteredHwUnitListMapped: MutableMap<HwUnit, List<String>?> = mutableMapOf()
                    filteredHwUnitList.forEach { (hwUnit: HwUnit, subType: String?) ->
                        when {
                            subType == null -> {
                                filteredHwUnitListMapped[hwUnit] = null
                            }
                            filteredHwUnitListMapped.containsKey(hwUnit) -> {
                                filteredHwUnitListMapped[hwUnit] = filteredHwUnitListMapped[hwUnit]?.plus(subType)
                            }
                            else -> {
                                filteredHwUnitListMapped[hwUnit] = listOf(subType)
                            }
                        }
                    }

                    val listOfFlows: List<Flow<Pair<List<HwUnitLog<Any?>>, List<String>?>>> = filteredHwUnitListMapped.map { hwUnitMapEntry ->
                        val flowList = mutableListOf<Flow<Map<String, HwUnitLog<Any?>>>>().also { list ->
                            // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
                            for (date in startRange..endRange step FULL_DAY_IN_MILLIS) {
                                list.add(homeRepository.logsFlow(hwUnitMapEntry.key.name, date).onCompletion {
                                    Timber.e("onCompletion")
                                    emit(mapOf())
                                }.onStart {
                                    Timber.e("onStart")
                                    emit(mapOf())
                                })
                            }
                        }
                        combine(flowList) { dailyMapArray ->
                            val combinedMap = Pair(mutableListOf<HwUnitLog<Any?>>(), hwUnitMapEntry.value)
                            dailyMapArray.forEach {
                                combinedMap.first.addAll(it.values)
                            }
                            combinedMap
                        }
                    }

                    combine(listOfFlows) { allFilteredHwUnitLogsData ->
                        Timber.e("logsFlow")
                        val lineDataSetList = mutableListOf<LineDataSet>()
                        val lineGradDataSetList = mutableListOf<LineDataSet>()
                        allFilteredHwUnitLogsData.forEach { (hwUnitLogsData, subTypesList) ->
                            hwUnitLogsData.let { hwUnitLogList ->
                                val hwUnitLog = hwUnitLogList.firstOrNull()
                                if (hwUnitLog != null) {
                                    when (hwUnitLog.type) {
                                        BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                                        BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                                            lineGradDataSetList.add(getBooleanGradSensorData(hwUnitLog.name, hwUnitLogList))
                                        }
                                        else -> {
                                            if (subTypesList == null) {
                                                lineDataSetList.add(getNumberSensorData(hwUnitLog.name, hwUnitLogList))
                                            } else {
                                                lineDataSetList.addAll(getMapNumberSensorData(hwUnitLog.name, subTypesList, hwUnitLogList))

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CombinedData().apply {
                            val lineDataSetColorFraction = 360f / lineDataSetList.size
                            lineDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyStyle(lineDataSetColorFraction * index)
                            }
                            val lineGradDataSetColorFraction = 360f / lineGradDataSetList.size
                            lineGradDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyGradStyle(lineGradDataSetColorFraction * index)
                            }

                            setData(LineData(lineDataSetList + lineGradDataSetList))
                        }
                    }
                }
            }

    fun clearLogs() = homeRepository.clearLog()

    @ExperimentalCoroutinesApi
    fun addFilter(hwUnitHash: Int): Boolean {
        return menuItemHwUnitListFlow.value.firstOrNull { it.second == hwUnitHash }?.let { (hwUnit, _, checked) ->
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

    private fun getNumberSensorData(hwUnitName: String, logsList: Collection<HwUnitLog<Any?>>): LineDataSet {
        val entries = logsList.sortedBy { it.servertime as Long }.mapNotNull { hwUnitLog ->
            val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
            val yValue: Float? = hwUnitLog.value?.let { hwValue ->
                when (hwValue) {
                    is Number -> hwValue.toFloat()
                    else -> null
                }
            }
            if (yValue != null) Entry(xValue, yValue) else null
        }
        return LineDataSet(entries, hwUnitName)
    }

    private fun getMapNumberSensorData(hwUnitName: String, mapNames: List<String>, logsList: Collection<HwUnitLog<Any?>>): List<LineDataSet> {
        val list: Map<String, MutableList<Entry>> = mapNames.associateWith { mutableListOf() }
        logsList.sortedBy { it.servertime as Long }.forEach { hwUnitLog ->
            hwUnitLog.value?.let { hwValue ->
                if (hwValue is Map<*, *>) {
                    val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
                    mapNames.forEach { innerHwUnitValueName ->
                        val yValue: Float? = hwValue[innerHwUnitValueName]?.let {
                            if (it is Number) it.toFloat() else null
                        }
                        if (yValue != null) {
                            list[innerHwUnitValueName]?.add(Entry(xValue, yValue))
                        }
                    }
                }
            }
        }
        return list.map {
            LineDataSet(it.value, "${hwUnitName}_${it.key}")
        }
    }

    private fun LineDataSet.applyStyle(fractionColor: Float) {
        val color = ColorUtils.HSLToColor(colorFloatArray.apply { set(0, fractionColor) })
        setColor(color)
        valueTextColor = color // styling, ...
        valueTextSize = 14f
        setCircleColor(color)
    }

    private fun getBooleanGradSensorData(hwUnitName: String, logsList: Collection<HwUnitLog<Any?>>): LineDataSet {
        val entries: MutableList<Entry> = mutableListOf()
        logsList.sortedBy { it.servertime as Long }.forEach { hwUnitLog ->
            hwUnitLog.value?.let { hwValue ->
                when (hwValue) {
                    is Boolean -> {
                        val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
                        if (hwValue) {
                            entries.add(Entry(xValue - Float.MIN_VALUE, 0f, hwValue))
                            entries.add(Entry(xValue, 40f, hwValue))
                        } else {
                            entries.add(Entry(xValue, 40f, hwValue))
                            entries.add(Entry(xValue + Float.MIN_VALUE, 0f, hwValue))
                        }
                    }
                }
            }
        }
        return LineDataSet(entries, hwUnitName)
    }

    private fun LineDataSet.applyGradStyle(fractionColor: Float) {
        val color = ColorUtils.HSLToColor(colorFloatArray.apply { set(0, fractionColor) })
        setColor(color)
        fillColor = color
        setCircleColor(color)
        setDrawFilled(true)
        valueTextSize = 16f
        setDrawValues(true)
    }
}

