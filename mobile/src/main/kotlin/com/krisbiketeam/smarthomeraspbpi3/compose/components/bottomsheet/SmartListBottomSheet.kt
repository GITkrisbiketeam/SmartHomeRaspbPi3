package com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartListBottomSheet(
    model: SmartListBottomSheetModel<*, *>,
    sheetState: SheetState,
    onDismissClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf(model.preselection) }

    ModalBottomSheet(
        onDismissRequest = onDismissClick, sheetState = sheetState
    ) {
        // Sheet content

        // Title
        Text(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
            text = stringResource(id = model.title)
        )

        // List
        LazyColumn(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .weight(1.0f, false)
                .padding(horizontal = dimensionResource(id = R.dimen.margin_normal))
        ) {
            items(model.list.size) { index ->
                val (itemText: String, itemChecked: Boolean?) = when (model) {
                    is SmartListBottomSheetModel.NonEmpty -> model.list[index] to null
                    is SmartListBottomSheetModel.NonEmptyUsed -> model.list[index].first to model.list[index].second
                    is SmartListBottomSheetModel.WithEmpty -> model.list[index] to null
                    is SmartListBottomSheetModel.WithEmptyUsed -> model.list[index].first to model.list[index].second
                }
                TextButton(
                    onClick = {
                        selectedItem = if (selectedItem != itemText) {
                            itemText
                        } else {
                            when (model) {
                                is SmartListBottomSheetModel.NonEmpty -> selectedItem
                                is SmartListBottomSheetModel.NonEmptyUsed -> selectedItem
                                is SmartListBottomSheetModel.WithEmpty -> null
                                is SmartListBottomSheetModel.WithEmptyUsed -> null
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (itemText == selectedItem) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.12f
                            ) else Color.Transparent
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = itemText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (itemChecked == true) {
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_block_24dp),
                                contentDescription = null, // decorative
                            )
                        }

                    }
                }
                HorizontalDivider()
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.margin_small))
                .align(Alignment.End)
        ) {

            TextButton(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.margin_normal)),
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissClick()
                    }
                }) {
                Text(stringResource(R.string.cancel))
            }

            TextButton(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            when (model) {
                                is SmartListBottomSheetModel.NonEmpty -> selectedItem?.let {
                                    model.positiveButtonAction(it)
                                }

                                is SmartListBottomSheetModel.NonEmptyUsed -> selectedItem?.let {
                                    model.positiveButtonAction(it)
                                }

                                is SmartListBottomSheetModel.WithEmpty -> model.positiveButtonAction(
                                    selectedItem
                                )

                                is SmartListBottomSheetModel.WithEmptyUsed -> model.positiveButtonAction(
                                    selectedItem
                                )
                            }
                        }
                    }
                },
                enabled = when (model) {
                    is SmartListBottomSheetModel.NonEmpty -> selectedItem != null
                    is SmartListBottomSheetModel.NonEmptyUsed -> selectedItem != null
                    is SmartListBottomSheetModel.WithEmpty -> true
                    is SmartListBottomSheetModel.WithEmptyUsed -> true
                }
            ) {
                Text(stringResource(model.positiveButtonTextId))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SmartAlertDialogPreview() {
    MaterialTheme {
        Surface {
            SmartListBottomSheet(
                model = SmartListBottomSheetModel.WithEmptyUsed(
                    title = R.string.save_room,
                    list = listOf("Item 1" to true, "Item 2" to false, "Item 3" to false),
                    positiveButtonTextId = R.string.menu_save,
                    positiveButtonAction = {},
                    preselection = "Item 2"
                ),
                sheetState = SheetState(
                    skipPartiallyExpanded = true, initialValue = SheetValue.Expanded,
                    positionalThreshold = { 0.0f },
                    velocityThreshold =  { 0.0f }
                ),
                onDismissClick = {},
            )
        }
    }
}