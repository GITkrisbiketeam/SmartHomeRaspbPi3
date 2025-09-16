package com.krisbiketeam.smarthomeraspbpi3.compose.components.daterangepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krisbiketeam.smarthomeraspbpi3.R
import java.util.Calendar

@ExperimentalMaterial3Api
@Composable
fun SmartDateRangePicker(
    startRangeTime: Long,
    endRangeTime: Long,
    onDismissRequest: () -> Unit,
    onSaveRequest: (Long?, Long?) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startRangeTime,
        initialSelectedEndDateMillis = endRangeTime,
        yearRange = IntRange(2020, Calendar.getInstance().get(Calendar.YEAR))
    )
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onDismissRequest() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Localized description")
                }
                TextButton(
                    onClick = {
                        onSaveRequest(state.selectedStartDateMillis, state.selectedEndDateMillis)
                    },
                    enabled = state.selectedEndDateMillis != null
                ) {
                    Text(text = stringResource(id = R.string.menu_save))
                }
            }
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SmartDateRangePickerPreview() {
    MaterialTheme {
        Surface {
            SmartDateRangePicker(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                //DateRangePickerState(null, null, null,IntRange(2020, 2024), DisplayMode.Picker),
                {},
                { _, _ -> })
        }
    }
}