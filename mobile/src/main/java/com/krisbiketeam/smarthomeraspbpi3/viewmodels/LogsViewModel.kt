package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.data.*
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import timber.log.Timber


/**
 * The ViewModel for [RoomListFragment].
 */
class LogsViewModel(application: Application, private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    @ExperimentalCoroutinesApi
    val logsData: LiveData<ChartData<*>> =
            combine(homeRepository.hwUnitListFlow(), homeRepository.logsFlow()) { hwUnitList, logsMap ->
                Timber.e("logsFlow")

                CombinedData().apply {
                    setData(LineData(getNumberSensorData(hwUnitList, logsMap)))
                    setData(ScatterData(getBooleanSensorData(hwUnitList, logsMap)))
                }
                //ScatterData(getBooleanSensorData(hwUnitList, logsMap))
            }.asLiveData(Dispatchers.IO)

    fun clearLogs() = homeRepository.clearLog()

    private fun getNumberSensorData(hwUnitList: List<HwUnit>, logsMap: Map<String, Map<String, HwUnitLog<Any?>>>): List<LineDataSet> {
        val floatArray = FloatArray(3) { idx ->
            when (idx) {
                1 -> 1f
                2 -> 0.5f
                else -> 0f
            }
        }
        var colorFraction: Float

        return hwUnitList.filter {
            //false
            it.type == BoardConfig.TEMP_SENSOR_MCP9808 ||
            it.type == BoardConfig.TEMP_RH_SENSOR_SI7021
        }.also {
            colorFraction = 360f / it.size
        }.mapIndexed { idx, hwUnit ->
            val entries = logsMap[hwUnit.name]?.values?.sortedBy { it.servertime as Long }?.mapNotNull { hwUnitLog ->
                val xValue: Float = (hwUnitLog.servertime as Number).toFloat()
                val yValue: Float? = hwUnitLog.value?.let { hwValue ->
                    when (hwValue) {
                        is Number -> hwValue.toFloat()
                        is Map<*, *> -> {
                            hwValue["humidity"]?.let { if (it is Number) it.toFloat() else null }
                        }
                        else -> null

                    }
                }
                if (yValue != null) Entry(xValue, yValue) else null
            } ?: emptyList()
            LineDataSet(entries, hwUnit.name).apply {
                val color = ColorUtils.HSLToColor(floatArray.apply { set(0, colorFraction * idx) })
                setColor(color)
                valueTextColor = color // styling, ...
                setCircleColor(color)
            }

        }
    }

    private fun getBooleanSensorData(hwUnitList: List<HwUnit>, logsMap: Map<String, Map<String, HwUnitLog<Any?>>>): List<ScatterDataSet> {
        val floatArray = FloatArray(3) { idx ->
            when (idx) {
                1 -> 1f
                2 -> 0.5f
                else -> 0f
            }
        }
        var colorFraction: Float

        return hwUnitList.filter {
            it.type == BoardConfig.IO_EXTENDER_MCP23017_OUTPUT ||
                    it.type == BoardConfig.IO_EXTENDER_MCP23017_INPUT
        }.also {
            colorFraction = 360f / it.size
        }.mapIndexed { idx, hwUnit ->
            val entries = logsMap[hwUnit.name]?.values?.sortedBy { it.servertime as Long }?.mapNotNull { hwUnitLog ->
                hwUnitLog.value?.let { hwValue ->
                    when (hwValue) {
                        is Boolean -> {
                            val xValue: Float = (hwUnitLog.servertime as Number).toFloat()
                            val yValue: Float = if (hwValue) 40f else 0f
                            Entry(xValue, yValue, hwValue)
                        }
                        else -> null

                    }
                }
            } ?: emptyList()
            ScatterDataSet(entries, hwUnit.name).apply {
                val color = ColorUtils.HSLToColor(floatArray.apply { set(0, colorFraction * idx) })
                setColor(color)
                setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                scatterShapeSize = 40f
                valueTextSize = 16f
                setDrawValues(true)
            }
        }
    }
}

