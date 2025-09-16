package com.krisbiketeam.smarthomeraspbpi3.compose.screens.logschart

import androidx.compose.ui.state.ToggleableState

data class LogsChartMenuFilterModel(val list: List<LogsChartMenuFilterTypesModel> = emptyList())

data class LogsChartMenuFilterTypesModel(
    val hwUnitGroupName: String,
    val selection: ToggleableState,
    val hwUnitNameList: List<Pair<String, Boolean>>
)