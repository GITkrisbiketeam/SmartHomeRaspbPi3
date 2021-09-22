package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.charts.ScatterChart
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
    val startRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    @ExperimentalCoroutinesApi
    val endRangeFlow: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())


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
            combine(startRangeFlow, endRangeFlow, filteredHwUnitListFlow) { startRange, endRange, filteredHwUnitList ->
                Triple(startRange, endRange, filteredHwUnitList)
            }.flatMapLatest { (startRange, endRange, filteredHwUnitList) ->
                if (filteredHwUnitList.isNullOrEmpty()) {
                    flowOf(CombinedData())
                } else {
                    val listOfFlows: List<Flow<Map<String, HwUnitLog<Any?>>>> = filteredHwUnitList.map { hwUnit ->
                        val flowList = mutableListOf<Flow<Map<String, HwUnitLog<Any?>>>>().also { list ->
                            // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
                            for (date in startRange..endRange step FULL_DAY_IN_MILLIS) {
                                list.add(homeRepository.logsFlow(hwUnit.name, date).onCompletion {
                                    Timber.e("onCompletion")
                                    emit(mapOf())
                                }.onStart {
                                    Timber.e("onStart")
                                    emit(mapOf())
                                })
                            }
                        }
                        combine(flowList) { dailyMapArray ->
                            val combinedMap = mutableMapOf<String, HwUnitLog<Any?>>()
                            dailyMapArray.forEach {
                                combinedMap.putAll(it)
                            }
                            combinedMap
                        }
                    }
                    combine(listOfFlows) { allFilteredHwUnitLogsData ->
                        Timber.e("logsFlow")
                        val lineDataSetList = mutableListOf<LineDataSet>()
                        //val scatterDataSetList = mutableListOf<ScatterDataSet>()
                        val lineGradDataSetList = mutableListOf<LineDataSet>()
                        allFilteredHwUnitLogsData.forEach { hwUnitLogsData ->
                            hwUnitLogsData.values.let { hwUnitLogList ->
                                val hwUnit = hwUnitLogList.firstOrNull()
                                when (hwUnit?.type) {
                                    BoardConfig.TEMP_SENSOR_MCP9808,
                                    BoardConfig.TEMP_SENSOR_TMP102-> {
                                        lineDataSetList.add(getNumberSensorData(hwUnit.name, hwUnitLogList))
                                    }
                                    BoardConfig.TEMP_RH_SENSOR_SI7021,
                                    BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                                        /*
                                        // TODO rename FirebaseTables temperatures to temperature name should be like in  TemperatureAndHumidity
                                        TemperatureAndHumidity::class.java.declaredFields.map {
                                            it.name
                                        }*/
                                        lineDataSetList.addAll(getMapNumberSensorData(hwUnit.name, listOf("temperature", "humidity"), hwUnitLogList))
                                    }
                                    BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                                        /*
                                        // TODO rename FirebaseTables breathVoc to breathVocEquivalent name should be like in  Bme680Data
                                        Bme680Data::class.java.declaredFields.map {
                                            it.name
                                        }*/
                                        lineDataSetList.addAll(getMapNumberSensorData(hwUnit.name, listOf("temperature", "humidity", "pressure", "iaq", "gas"), hwUnitLogList))
                                    }
                                    BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                                    BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                                        //scatterDataSetList.add(getBooleanSensorData(hwUnit.name, hwUnitLogList))
                                        lineGradDataSetList.add(getBooleanGradSensorData(hwUnit.name, hwUnitLogList))
                                    }
                                }
                            }
                        }
                        CombinedData().apply {
                            val lineDataSetColorFraction = 360f / lineDataSetList.size
                            lineDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyStyle(lineDataSetColorFraction * index)
                            }
                            /*val scatterDataSetColorFraction = 360f / scatterDataSetList.size
                            scatterDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyStyle(scatterDataSetColorFraction * index)
                            }*/
                            val lineGradDataSetColorFraction = 360f / lineGradDataSetList.size
                            lineGradDataSetList.forEachIndexed { index, lineDataSet ->
                                lineDataSet.applyGradStyle(lineGradDataSetColorFraction * index)
                            }

                            setData(LineData(lineDataSetList + lineGradDataSetList))
                            //setData(ScatterData(scatterDataSetList.toList()))
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

    private fun ScatterDataSet.applyStyle(fractionColor: Float) {
        val color = ColorUtils.HSLToColor(colorFloatArray.apply { set(0, fractionColor) })
        setColor(color)
        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        scatterShapeSize = 40f
        valueTextSize = 16f
        setDrawValues(true)
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

