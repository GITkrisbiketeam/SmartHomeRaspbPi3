package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.data.*
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
class LogsViewModel(application: Application, private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    private val colorFloatArray = FloatArray(3) { idx ->
        when (idx) {
            1 -> 1f
            2 -> 0.5f
            else -> 0f
        }
    }

    @ExperimentalCoroutinesApi
    private val filteredHwUnitListFlow: MutableStateFlow<List<HwUnit>> = MutableStateFlow(emptyList())

    @ExperimentalCoroutinesApi
    val menuItemHwUnitListFlow: StateFlow<List<Triple<HwUnit, Int, Boolean>>> =
            combine(filteredHwUnitListFlow, homeRepository.hwUnitListFlow()) { filteredHwUnitList, hwUnitList ->
                hwUnitList.map {
                    Triple(it, it.hashCode(), filteredHwUnitList.contains(it))
                }
            }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    emptyList()
            )

    @ExperimentalCoroutinesApi
    val logsData: Flow<ChartData<*>> =
            filteredHwUnitListFlow.flatMapLatest { filteredHwUnitList ->
                if (filteredHwUnitList.isNullOrEmpty()) {
                    flowOf(CombinedData())
                } else {
                    combine(filteredHwUnitList.map { homeRepository.logsFlow(it.name) }) { allFilteredHwUnitLogsData ->
                        Timber.e("logsFlow")
                        val lineDataSetList = mutableListOf<LineDataSet>()
                        val scatterDataSetList = mutableListOf<ScatterDataSet>()
                        allFilteredHwUnitLogsData.forEach { hwUnitLogsData ->
                            hwUnitLogsData.values.flatMap(Map<String, HwUnitLog<Any?>>::values).let { hwUnitLogList ->
                                val hwUnit = hwUnitLogList.firstOrNull()
                                when (hwUnit?.type) {
                                    BoardConfig.TEMP_SENSOR_MCP9808 -> {
                                        lineDataSetList.add(getNumberSensorData(hwUnit.name, hwUnitLogList))
                                    }
                                    BoardConfig.TEMP_RH_SENSOR_SI7021 -> {
                                        lineDataSetList.addAll(getMapNumberSensorData(hwUnit.name, listOf("temperature", "humidity"), hwUnitLogList))
                                    }
                                    BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                                    BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                                        scatterDataSetList.add(getBooleanSensorData(hwUnit.name, hwUnitLogList))
                                    }
                                }
                            }
                        }
                        CombinedData().apply {
                            val lineDataSetColorFraction = 360f / lineDataSetList.size
                            lineDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyStyle(lineDataSetColorFraction * index)
                            }
                            val scatterDataSetColorFraction = 360f / scatterDataSetList.size
                            scatterDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyStyle(scatterDataSetColorFraction * index)
                            }
                            setData(LineData(lineDataSetList.toList()))
                            setData(ScatterData(scatterDataSetList.toList()))
                        }
                    }
                }
            }

    fun clearLogs() = homeRepository.clearLog()

    @ExperimentalCoroutinesApi
    fun addFilter(hwUnitHash: Int): Boolean {
        return menuItemHwUnitListFlow.value.firstOrNull { it.second == hwUnitHash }?.let { (hwUnit, _, _) ->
            val newList = filteredHwUnitListFlow.value.toMutableList()
            if (newList.contains(hwUnit)) {
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

    private fun getBooleanSensorData(hwUnitName: String, logsList: Collection<HwUnitLog<Any?>>): ScatterDataSet {
        val entries = logsList.sortedBy { it.servertime as Long }.mapNotNull { hwUnitLog ->
            hwUnitLog.value?.let { hwValue ->
                when (hwValue) {
                    is Boolean -> {
                        val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
                        val yValue: Float = if (hwValue) 40f else 0f
                        Entry(xValue, yValue, hwValue)
                    }
                    else -> null
                }
            }
        }
        return ScatterDataSet(entries, hwUnitName)
    }

    fun ScatterDataSet.applyStyle(fractionColor: Float) {
        val color = ColorUtils.HSLToColor(colorFloatArray.apply { set(0, fractionColor) })
        setColor(color)
        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        scatterShapeSize = 40f
        valueTextSize = 16f
        setDrawValues(true)
    }
}

