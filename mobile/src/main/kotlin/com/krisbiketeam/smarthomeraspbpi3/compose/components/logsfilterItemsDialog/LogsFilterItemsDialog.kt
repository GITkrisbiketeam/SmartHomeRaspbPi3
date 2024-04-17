package com.krisbiketeam.smarthomeraspbpi3.compose.components.logsfilterItemsDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.logschart.LogsChartMenuFilterModel

@ExperimentalMaterial3Api
@Composable
fun LogsFilterItemsDialog(
    menuItems: LogsChartMenuFilterModel,
    onDismissRequest: () -> Unit,
    onSaveRequest: (MutableMap<String, MutableMap<String, Boolean>>) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    // Align the title to the center when an icon is present.
                    Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = "Filter HwUnits Logs",
                        color = AlertDialogDefaults.titleContentColor,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                val resultingHwUnitFilteredMap: MutableMap<String, MutableMap<String, Boolean>> =
                    menuItems.list.associate { group ->
                        group.hwUnitGroupName to group.hwUnitNameList.toMap().toMutableMap()
                    }.toMutableMap()

                LazyColumn(modifier = Modifier.weight(1f)) {
                    resultingHwUnitFilteredMap.entries.forEach { (hwUnitTypeName, hwUnitNameMap) ->
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            var rememberedHwUnitNameMap by remember {
                                mutableStateOf(
                                    hwUnitNameMap, neverEqualPolicy()
                                )
                            }

                            val checkState = checkState(rememberedHwUnitNameMap)

                            Row(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TriStateCheckbox(checkState, onClick = {
                                    when (checkState) {
                                        ToggleableState.Off, ToggleableState.Indeterminate -> {
                                            hwUnitNameMap.forEach { (hwUnitName, _) ->
                                                hwUnitNameMap[hwUnitName] = true
                                            }
                                        }

                                        ToggleableState.On -> {
                                            hwUnitNameMap.forEach { (hwUnitName, _) ->
                                                hwUnitNameMap[hwUnitName] = false
                                            }
                                        }
                                    }
                                    // causes recomposition
                                    rememberedHwUnitNameMap = hwUnitNameMap
                                    resultingHwUnitFilteredMap[hwUnitTypeName] = hwUnitNameMap
                                })
                                Text(
                                    text = hwUnitTypeName,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown,
                                    null,
                                    Modifier
                                        .rotate(if (expanded) 180f else 0f)
                                        .clickable(onClick = { expanded = !expanded }))
                            }

                            if (expanded) {
                                rememberedHwUnitNameMap.forEach { (hwUnitName, checked) ->
                                    DropdownMenuItem(onClick = {
                                        hwUnitNameMap[hwUnitName] = !checked
                                        // causes recomposition
                                        rememberedHwUnitNameMap = hwUnitNameMap
                                        resultingHwUnitFilteredMap[hwUnitTypeName] = hwUnitNameMap
                                    },
                                        text = {
                                            Text(text = hwUnitName)
                                        },
                                        trailingIcon = {
                                            Checkbox(checked = checked, onCheckedChange = {
                                                hwUnitNameMap[hwUnitName] = !checked
                                                // causes recomposition
                                                rememberedHwUnitNameMap = hwUnitNameMap
                                                resultingHwUnitFilteredMap[hwUnitTypeName] =
                                                    hwUnitNameMap
                                            })
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                    }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        onSaveRequest(resultingHwUnitFilteredMap)
                    }) {
                        Text("Ok")
                    }
                }
            }
        }
    }
}

private fun checkState(map: Map<String, Boolean>): ToggleableState {
    return when {
        map.all { it.value } -> ToggleableState.On
        map.any { it.value } -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SmartDateRangePickerPreview() {
    MaterialTheme {
        Surface {
            LogsFilterItemsDialog(LogsChartMenuFilterModel(), {}, {})
        }
    }
}