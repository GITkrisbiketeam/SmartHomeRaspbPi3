@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.krisbiketeam.smarthomeraspbpi3.compose.screens.logschart

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyDateLocalTime
import com.krisbiketeam.smarthomeraspbpi3.compose.components.daterangepicker.SmartDateRangePicker
import com.krisbiketeam.smarthomeraspbpi3.compose.components.logsfilterItemsDialog.LogsFilterItemsDialog
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.LogsChartTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsFloat
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsLong
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsChartScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    preselection: Pair<String, String>? = null,
    viewModel: LogsChartViewModel = koinViewModel { parametersOf(preselection) },
) {
    val startRangeDate by viewModel.startRangeFlow.collectAsStateWithLifecycle()
    val endRangeDate by viewModel.endRangeFlow.collectAsStateWithLifecycle()

    var smartDateRangePickerShown by remember { mutableStateOf(false) }
    if (smartDateRangePickerShown) {
        SmartDateRangePicker(
            startRangeDate,
            endRangeDate,
            { smartDateRangePickerShown = false },
            { startTime, endTime ->
                startTime?.let { viewModel.startRangeFlow.value = it }
                endTime?.let { viewModel.endRangeFlow.value = it }
                smartDateRangePickerShown = false
            })
    }

    val logsChartMenuFilterModel by viewModel.menuItemHwUnitListFlow.collectAsStateWithLifecycle()

    var filterItemsDialogShown by remember { mutableStateOf(false) }
    if (filterItemsDialogShown) {
        LogsFilterItemsDialog(
            logsChartMenuFilterModel,
            { filterItemsDialogShown = false },
            { resultingHwUnitFilteredMap ->
                viewModel.setFilters(resultingHwUnitFilteredMap)
                filterItemsDialogShown = false
            })
    }

    val lineData by viewModel.logsData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LogsChartTopAppBar(
                openDrawer = openDrawer,
                onPickDateClicked = { smartDateRangePickerShown = true },
                onFilterLogsClicked = { filterItemsDialogShown = true },
                onClearAll = { viewModel.clearLogs() },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { context ->
                CombinedChart(context).apply {
                    setHardwareAccelerationEnabled(true)
                    setNoDataText(context.getString(R.string.logs_fragment_no_data))
                    isKeepPositionOnRotation = true
                    description = null
                    legend.isWordWrapEnabled = true

                    xAxis.apply {
                        setLabelCount(4, false)
                        //granularity = 24*60 * 60 * 1000f // minimum axis-step (interval) is 1
                        val timeFormat = SimpleDateFormat("EE d HH:mm:ss", Locale.getDefault())
                        valueFormatter = object : ValueFormatter() {
                            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                                return timeFormat.format(Date(value.toLogsLong()))
                            }
                        }
                    }
                    setDrawGridBackground(false)
                    invalidate()
                }
            },
            update = { chart ->
                if (lineData.dataSetCount > 0) {
                    Timber.d("subscribeLogsData lineData: ${lineData.dataSetCount} ${lineData.entryCount}")
                    chart.data = lineData
                    chart.xAxis.axisMinimum =
                        lineData.xMin.toLogsLong().getOnlyDateLocalTime().toLogsFloat()
                    chart.xAxis.axisMaximum = (lineData.xMax.toLogsLong()
                        .getOnlyDateLocalTime() + (2 * FULL_DAY_IN_MILLIS)).toLogsFloat()
                    chart.invalidate() // refresh
                }
            })

    }
}

// region previews
/*

@Preview
@Composable
private fun TasksContentPreview() {
    MaterialTheme {
        Surface {
            TasksContent(
                loading = false,
                tasks = listOf(
                    Task(
                        title = "Title 1",
                        description = "Description 1",
                        isCompleted = false,
                        id = "ID 1"
                    ),
                    Task(
                        title = "Title 2",
                        description = "Description 2",
                        isCompleted = true,
                        id = "ID 2"
                    ),
                    Task(
                        title = "Title 3",
                        description = "Description 3",
                        isCompleted = true,
                        id = "ID 3"
                    ),
                    Task(
                        title = "Title 4",
                        description = "Description 4",
                        isCompleted = false,
                        id = "ID 4"
                    ),
                    Task(
                        title = "Title 5",
                        description = "Description 5",
                        isCompleted = true,
                        id = "ID 5"
                    ),
                ),
                currentFilteringLabel = R.string.label_all,
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill,
                onRefresh = { },
                onTaskClick = { },
                onTaskCheckedChange = { _, _ -> },
            )
        }
    }
}

@Preview
@Composable
private fun TasksContentEmptyPreview() {
    MaterialTheme {
        Surface {
            TasksContent(
                loading = false,
                tasks = emptyList(),
                currentFilteringLabel = R.string.label_all,
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill,
                onRefresh = { },
                onTaskClick = { },
                onTaskCheckedChange = { _, _ -> },
            )
        }
    }
}

@Preview
@Composable
private fun TasksEmptyContentPreview() {
    MaterialTheme {
        Surface {
            TasksEmptyContent(
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill
            )
        }
    }
}

@Preview
@Composable
private fun TaskItemPreview() {
    MaterialTheme {
        Surface {
            TaskItem(
                task = Task(
                    title = "Title",
                    description = "Description",
                    id = "ID"
                ),
                onTaskClick = { },
                onCheckedChange = { }
            )
        }
    }
}

@Preview
@Composable
private fun TaskItemCompletedPreview() {
    MaterialTheme {
        Surface {
            TaskItem(
                task = Task(
                    title = "Title",
                    description = "Description",
                    isCompleted = true,
                    id = "ID"
                ),
                onTaskClick = { },
                onCheckedChange = { }
            )
        }
    }
}
*/

// endregion