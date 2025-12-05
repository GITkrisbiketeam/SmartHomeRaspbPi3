package com.krisbiketeam.smarthomeraspbpi3.compose.screens.logschart

import androidx.compose.ui.state.ToggleableState
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.*
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyDateLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig.IO_EXTENDER_MCP23017_INPUT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig.IO_EXTENDER_MCP23017_OUTPUT
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.toHomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The ViewModel for [RoomListFragment].
 */
class LogsChartViewModel(
    private val homeRepository: FirebaseHomeInformationRepository,
    preselection: Pair<String, String>? = null
) :
    ViewModel() {

    init {
        Timber.e("LogsChartViewModel preselection:$preselection")
    }

    private val colorFloatArray = FloatArray(3) { idx ->
        when (idx) {
            1 -> 1f
            2 -> 0.5f
            else -> 0f
        }
    }

    // List of HwUnits with their value name ex. temperature or humidity
    private val filteredHwUnitListFlow: MutableStateFlow<List<Pair<String, String>>> =
        MutableStateFlow(preselection?.let { (hwUnitName, homeUnitType) ->
            listOf(hwUnitName to homeUnitType.let {
                when (it.toHomeUnitType()) {
                    HomeUnitType.HOME_ACTUATORS -> IO_EXTENDER_MCP23017_OUTPUT
                    HomeUnitType.HOME_BLINDS -> IO_EXTENDER_MCP23017_OUTPUT
                    HomeUnitType.HOME_REED_SWITCHES -> IO_EXTENDER_MCP23017_INPUT
                    HomeUnitType.HOME_MOTIONS -> IO_EXTENDER_MCP23017_INPUT
                    HomeUnitType.HOME_LIGHT_SWITCHES -> IO_EXTENDER_MCP23017_OUTPUT
                    HomeUnitType.HOME_WATER_CIRCULATION -> IO_EXTENDER_MCP23017_OUTPUT
                    HomeUnitType.HOME_MCP23017_WATCH_DOG -> IO_EXTENDER_MCP23017_OUTPUT
                    else -> it
                }
            })
        } ?: emptyList())

    val startRangeFlow: MutableStateFlow<Long> =
        MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    val endRangeFlow: MutableStateFlow<Long> =
        MutableStateFlow(System.currentTimeMillis().getOnlyDateLocalTime())

    val menuItemHwUnitListFlow: StateFlow<LogsChartMenuFilterModel> = combine(
        filteredHwUnitListFlow, getHwUnitList()
    ) { filteredHwUnitList, hwUnitList ->

        LogsChartMenuFilterModel(hwUnitList.map { (hwUnitGroup, hwUnitList) ->
            LogsChartMenuFilterTypesModel(
                hwUnitGroup,
                ToggleableState.Off,
                hwUnitList.map { hwUnitName ->
                    hwUnitName to filteredHwUnitList.any { (filteredHwUnitName, filteredHwUnitGroup) ->
                        hwUnitGroup == filteredHwUnitGroup && filteredHwUnitName == hwUnitName
                    }
                })
        })
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), LogsChartMenuFilterModel()
    )

    @ExperimentalCoroutinesApi
    val logsData: StateFlow<CombinedData> = combine(
        startRangeFlow, endRangeFlow, filteredHwUnitListFlow
    ) { startRange, endRange, filteredHwUnitList ->
        Triple(startRange, endRange, filteredHwUnitList)
    }.flatMapLatest { (startRange, endRange, filteredHwUnitList) ->
        if (filteredHwUnitList.isEmpty()) {
            flowOf(CombinedData())
        } else {
            // build Map of HwUnits to List of hwUnit type groups (this is not hwUnit.type)
            val filteredHwUnitListMapped: Map<String, List<String>> = buildMap {
                filteredHwUnitList.forEach { (hwUnitName: String, hwUnitTypeGroup: String) ->
                    computeIfPresent(hwUnitName) { _, value ->
                        value.plus(hwUnitTypeGroup)
                    }
                    computeIfAbsent(hwUnitName) {
                        listOf(hwUnitTypeGroup)
                    }
                }
            }
            Timber.i("logsData filteredHwUnitListMapped:$filteredHwUnitListMapped")

            // Build list of Flows of Pairs of List
            val listOfFlows: List<Flow<Pair<List<HwUnitLog<Any?>>, List<String>>>> =
                filteredHwUnitListMapped.map { (hwUnitName, hwUnitTypeGroupList) ->
                    // List of Flows of logs for given HwUnit for every day in a range.
                    // This will be Map of time to HwUnitLog for given HwUnit (hwUnitName)
                    val flowList = buildList<Flow<Map<String, HwUnitLog<Any?>>>> {
                        // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
                        for (date in startRange..endRange step FULL_DAY_IN_MILLIS) {
                            add(homeRepository.logsFlow(hwUnitName, date)
                                .onCompletion {
                                    Timber.e("onCompletion $hwUnitName $date")
                                    emit(mapOf())
                                }.onStart {
                                    //Timber.e("onStart $hwUnitName $date")
                                    emit(mapOf())
                                })
                        }
                    }
                    Timber.i("logsData $hwUnitName Combine all days logs into one, size:${flowList.size}")
                    // Combine all days logs into one flow
                    combine(flowList) { dailyMapArray ->
                        val logsList = buildList {
                            dailyMapArray.forEach {
                                addAll(it.values)
                            }
                        }
                        Pair(logsList, hwUnitTypeGroupList)
                    }
                }

            Timber.i("logsData Combine all HwUnits days logs into one, size:${listOfFlows.size}")
            combine(listOfFlows) { allFilteredHwUnitLogsData ->
                val lineDataSetList = mutableListOf<LineDataSet>()
                val lineGradDataSetList = mutableListOf<LineDataSet>()

                Timber.i("logsData start adding logs data to Chart CombinedData for hwUnits: ${allFilteredHwUnitLogsData.size}")
                allFilteredHwUnitLogsData.forEach { (hwUnitLogsList, hwUnitTypeGroupList) ->
                    // check type of hwUnit from first HwUnitLog
                    hwUnitLogsList.firstOrNull()?.let { hwUnitLog ->
                        when (hwUnitLog.type) {
                            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
                            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                                lineGradDataSetList.add(
                                    getBooleanGradSensorData(
                                        hwUnitLog.name, hwUnitLogsList
                                    )
                                )
                            }

                            else -> {
                                lineDataSetList.addAll(
                                    getMapNumberSensorData(
                                        hwUnitLog, hwUnitTypeGroupList, hwUnitLogsList
                                    )
                                )
                            }
                        }
                    }
                }

                Timber.i("logsData apply colors to Chart CombinedData")
                CombinedData().apply {
                    val lineDataSetColorFraction = 360f / lineDataSetList.size
                    lineDataSetList.forEachIndexed { index, lineDataSet ->
                        lineDataSet.applyStyle(lineDataSetColorFraction * index)
                    }
                    val lineGradDataSetColorFraction = 360f / lineGradDataSetList.size
                    lineGradDataSetList.forEachIndexed { index, lineDataSet ->
                        lineDataSet.applyGradStyle(lineGradDataSetColorFraction * index)
                    }

                    Timber.i("logsData setData")
                    setData(LineData(lineDataSetList + lineGradDataSetList))
                }
            }
        }
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), CombinedData()
    )

    fun clearLogs() {
        viewModelScope.launch {
            homeRepository.hwUnitListFlow().collect { hwUnitList ->
                for (date in startRangeFlow.value..endRangeFlow.value step FULL_DAY_IN_MILLIS) {
                    hwUnitList.forEach { hwUnit ->
                        Timber.e("clear logs for ${hwUnit.name} on $date")
                        homeRepository.clearHwUnitLogs(hwUnit.name, date.toString())
                            ?.addOnSuccessListener {
                                Timber.e("FINISHED clear logs for ${hwUnit.name} on $date")
                            }
                    }

                }
            }
        }
        homeRepository.clearAllThingsLog()
    }

    fun setFilters(resultingHwUnitFilteredMap: MutableMap<String, MutableMap<String, Boolean>>) {
        viewModelScope.launch(Dispatchers.IO) {
            filteredHwUnitListFlow.value = buildList {
                resultingHwUnitFilteredMap.forEach { (hwUnitTypeGroup, hwUnitList) ->
                    hwUnitList.forEach { (hwUnitName, checked) ->
                        if (checked) {
                            add(hwUnitName to hwUnitTypeGroup)
                        }
                    }
                }
            }.also { Timber.w("setFilters $it") }
        }
    }

    /**
     * return Flow of Map of hwUnitGroup to List of hwUnits
     */
    private fun getHwUnitList(): Flow<Map<String, List<String>>> =
        homeRepository.hwUnitListFlow().map { hwUnitList ->
            buildMap<String, MutableList<String>> {
                hwUnitList.map { hwUnit ->
                    when (hwUnit.type) {
                        BoardConfig.TEMP_RH_SENSOR_SI7021, BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                            listOf(
                                HomeUnitType.HOME_TEMPERATURES.firebaseTableName,
                                HomeUnitType.HOME_HUMIDITY.firebaseTableName
                            )
                        }

                        BoardConfig.PRESS_TEMP_SENSOR_LPS331 -> {
                            listOf(
                                HomeUnitType.HOME_TEMPERATURES.firebaseTableName,
                                HomeUnitType.HOME_PRESSURES.firebaseTableName
                            )
                        }

                        BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                            listOf(
                                HomeUnitType.HOME_TEMPERATURES.firebaseTableName,
                                HomeUnitType.HOME_PRESSURES.firebaseTableName,
                                HomeUnitType.HOME_HUMIDITY.firebaseTableName,
                                HomeUnitType.HOME_IAQ.firebaseTableName,
                                HomeUnitType.HOME_GAS.firebaseTableName,
                                HomeUnitType.HOME_STATIC_IAQ.firebaseTableName,
                                HomeUnitType.HOME_CO2.firebaseTableName,
                                HomeUnitType.HOME_BREATH_VOC.firebaseTableName,
                                HomeUnitType.HOME_GAS_PERCENT.firebaseTableName
                            )
                        }

                        BoardConfig.TEMP_SENSOR_MCP9808, BoardConfig.TEMP_SENSOR_TMP102 -> {
                            listOf(HomeUnitType.HOME_TEMPERATURES.firebaseTableName)
                        }

                        else -> {
                            listOf(hwUnit.type)
                        }
                    }.forEach { group ->
                        computeIfPresent(group) { _, list: MutableList<String> ->
                            list.apply { add(hwUnit.name) }
                        }
                        computeIfAbsent(group) {
                            mutableListOf(hwUnit.name)
                        }
                    }
                }
            }
        }

    private fun getMapNumberSensorData(
        baseHwUnitLog: HwUnitLog<Any?>,
        hwUnitTypeGroupList: List<String>,
        logsList: Collection<HwUnitLog<Any?>>
    ): List<LineDataSet> {
        val mapNames = hwUnitTypeGroupList.map {
            mapHwUnitTypeGroupToHwUnitLogValueType(
                it,
                baseHwUnitLog.type
            )
        }
        val hwUnitLogsTypeMap: Map<String?, MutableList<Entry>> =
            mapNames.associateWith { mutableListOf() }
        logsList.sortedBy { it.servertime as Long }.forEach { hwUnitLog ->
            hwUnitLog.value?.let { hwValue ->
                if (hwValue is Map<*, *>) {
                    val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
                    mapNames.forEach { innerHwUnitValueName ->
                        val yValue: Float? = hwValue[innerHwUnitValueName]?.let {
                            if (it is Number) it.toFloat() else null
                        }
                        if (yValue != null) {
                            hwUnitLogsTypeMap[innerHwUnitValueName]?.add(Entry(xValue, yValue))
                        }
                    }
                } else {
                    val xValue: Float = (hwUnitLog.servertime as Number).toLogsFloat()
                    val yValue: Float? = hwUnitLog.value?.let { hwValue ->
                        if (hwValue is Number) hwValue.toFloat() else null
                    }
                    if (yValue != null) {
                        hwUnitLogsTypeMap[null]?.add(Entry(xValue, yValue))
                    }
                }
            }
        }
        return hwUnitLogsTypeMap.map { entry ->
            LineDataSet(entry.value, "${baseHwUnitLog.name}${entry.key?.let { "_$it" } ?: ""}")
        }
    }

    private fun mapHwUnitTypeGroupToHwUnitLogValueType(
        hwUnitTypeGroup: String, hwUnitType: String
    ): String? {
        return when (hwUnitType) {
            BoardConfig.TEMP_RH_SENSOR_SI7021, BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                when (hwUnitTypeGroup) {
                    HomeUnitType.HOME_TEMPERATURES.firebaseTableName -> "temperature"
                    HomeUnitType.HOME_HUMIDITY.firebaseTableName -> "humidity"
                    else -> null
                }
            }

            BoardConfig.PRESS_TEMP_SENSOR_LPS331 -> {
                when (hwUnitTypeGroup) {
                    HomeUnitType.HOME_TEMPERATURES.firebaseTableName -> "temperature"
                    HomeUnitType.HOME_PRESSURES.firebaseTableName -> "pressure"
                    else -> null
                }
            }

            BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                when (hwUnitTypeGroup) {
                    HomeUnitType.HOME_TEMPERATURES.firebaseTableName -> "temperature"
                    HomeUnitType.HOME_HUMIDITY.firebaseTableName -> "humidity"
                    HomeUnitType.HOME_PRESSURES.firebaseTableName -> "pressure"
                    HomeUnitType.HOME_IAQ.firebaseTableName -> "iaq"
                    HomeUnitType.HOME_GAS.firebaseTableName -> "gas"
                    HomeUnitType.HOME_STATIC_IAQ.firebaseTableName -> "staticIaq"
                    HomeUnitType.HOME_CO2.firebaseTableName -> "co2Equivalent"
                    HomeUnitType.HOME_BREATH_VOC.firebaseTableName -> "breathVocEquivalent"
                    HomeUnitType.HOME_GAS_PERCENT.firebaseTableName -> "gasPercentage"
                    else -> null
                }
            }

            else -> {
                null
            }
        }

    }

    private fun LineDataSet.applyStyle(fractionColor: Float) {
        val color = ColorUtils.HSLToColor(colorFloatArray.apply { set(0, fractionColor) })
        setColor(color)
        valueTextColor = color // styling, ...
        valueTextSize = 14f
        setCircleColor(color)
    }

    private fun getBooleanGradSensorData(
        hwUnitName: String, logsList: Collection<HwUnitLog<Any?>>
    ): LineDataSet {
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

