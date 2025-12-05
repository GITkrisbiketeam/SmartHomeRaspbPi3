package com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RISING_EDGE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_BOOLEAN_APPLY
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HW_UNIT
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.toHomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartAlertDialog
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartListBottomSheet
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.HomeUnitDetailTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.Editable
import com.krisbiketeam.smarthomeraspbpi3.utils.getDayTime
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun HomeUnitScreen(
    roomName: String?,
    homeUnitName: String,
    homeUnitType: String,
    navigateToUnitTask: (String) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeUnitScreenViewModel = koinViewModel {
        parametersOf(
            roomName, homeUnitName, homeUnitType.toHomeUnitType()
        )
    },
) {

    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditMode.collectAsStateWithLifecycle()

    LaunchedEffect("navigateUp") {
        viewModel.navigateUp.collect {
            navigateUp.invoke()
        }
    }
    LaunchedEffect("snackBar") {
        viewModel.snackBarText.collect {
            snackBarHostState.showSnackbar(context.getString(it))
        }
    }

    HomeUnitScreenImpl(
        snackBarHostState = snackBarHostState,
        uiState = uiState,
        isEditing,
        startEditing = viewModel::startEditing,
        changeUnitName = viewModel::changeUnitName,
        closeAlertDialog = viewModel::closeAlertDialog,
        closeListBottomSheet = viewModel::closeListBottomSheet,
        actionSave = viewModel::actionSave,
        actionDiscard = viewModel::actionDiscard,
        actionDeleteRoom = viewModel::actionDeleteHomeUnit,
        setFirebaseNotify = viewModel::setFirebaseNotify,
        setShowInTaskList = viewModel::setShowInTaskList,
        navigateToUnitTask = navigateToUnitTask,
        navigateUp = navigateUp,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUnitScreenImpl(
    snackBarHostState: SnackbarHostState,
    uiState: HomeUnitScreenUiState,
    isEditing: Boolean,
    startEditing: () -> Unit,
    changeUnitName: (String) -> Unit,
    closeAlertDialog: () -> Unit,
    closeListBottomSheet: () -> Unit,
    actionSave: () -> Unit,
    actionDiscard: () -> Unit,
    actionDeleteRoom: () -> Unit,
    setFirebaseNotify: (Boolean) -> Unit,
    setShowInTaskList: (Boolean) -> Unit,
    navigateToUnitTask: (String) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            HomeUnitDetailTopAppBar(
                onBack = navigateUp,
                isEditing = isEditing,
                onEditClicked = { startEditing() },
                onDone = actionSave,
                onDiscard = actionDiscard,
                onDelete = actionDeleteRoom
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // region Unit Name
            item {
                if (isEditing) {
                    var text by rememberSaveable { mutableStateOf(uiState.unitName) }

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(id = R.dimen.margin_normal)),
                        value = text,
                        onValueChange = {
                            text = it
                            changeUnitName(it)
                        })
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                        text = uiState.unitName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.size(dimensionResource(id = R.dimen.margin_normal)))
            }
            // endregion

            // region value

            if (uiState.value != null) {
                item {
                    GeneralHomeUnitValue(
                        context = LocalContext.current,
                        homeUnitValue = uiState.value
                    )
                }
            }

            if (uiState.lastTriggerSource != null) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                        text = stringResource(id = R.string.add_edit_home_unit_text_last_trigger_source_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_large),
                                end = dimensionResource(id = R.dimen.margin_normal)
                            ), text = uiState.lastTriggerSource
                    )
                }
            }

            // endregion

            // region Unit Type
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                    text = stringResource(id = R.string.add_edit_home_unit_text_unit_type_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_large),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        )
                        .clickable(enabled = isEditing) {
                            uiState.unitType.editAction()
                        },
                    text = uiState.unitType.value ?: "N/A"
                )
            }
            // endregion

            // region Room Name
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                    text = stringResource(id = R.string.add_edit_home_unit_text_room_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                val roomName = uiState.roomName
                if (roomName.value != null || isEditing) {
                    Text(
                        modifier = Modifier
                            .clickable(enabled = isEditing) {
                                roomName.editAction()
                            }
                            .fillMaxWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_large),
                                end = dimensionResource(id = R.dimen.margin_normal)
                            ),
                        text = roomName.value ?: "N/A"
                    )
                }
            }
            // endregion

            // region HW Unit Name
            item {
                HomeUnitHWUnits(hwUnitState = uiState.hwUnit, isEditing = isEditing)
            }
            // endregion

            // region Additional Settings
            if (uiState.additionalSettings != null) {
                item {
                    HomeUnitAdditionalSettings(uiState.additionalSettings)
                }
            }
            // endregion

            // region FirebaseNotify Switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(end = dimensionResource(id = R.dimen.margin_small)),
                        text = stringResource(id = R.string.add_edit_home_unit_notify_firebase_switch_text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = uiState.firebaseNotify,
                        enabled = isEditing,
                        onCheckedChange = setFirebaseNotify
                    )
                }
                if (uiState.firebaseNotifyTrigger != null && uiState.firebaseNotify) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                        text = stringResource(id = R.string.unit_task_text_trigger_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        modifier = Modifier
                            .clickable(enabled = isEditing) {
                                uiState.firebaseNotifyTrigger.editAction()
                            }
                            .fillMaxWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_large),
                                end = dimensionResource(id = R.dimen.margin_normal)
                            ),
                        text = uiState.firebaseNotifyTrigger.value ?: "N/A"
                    )
                }
            }
            // endregion

            // region Show in Task List Switch
            if (uiState.value is HomeUnitScreenValueUiState.SwitchValueUiState) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(end = dimensionResource(id = R.dimen.margin_small)),
                            text = stringResource(id = R.string.add_edit_home_unit_show_in_task_list_switch_text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Switch(
                            checked = uiState.showInTaskList,
                            enabled = isEditing,
                            onCheckedChange = setShowInTaskList
                        )
                    }
                }
            }
            // endregion

            // region Task List
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                    text = stringResource(id = R.string.add_edit_home_unit_text_task_list_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                uiState.unitTasks.forEach { taskListItem ->
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_large),
                                end = dimensionResource(id = R.dimen.margin_normal),
                                top = dimensionResource(id = R.dimen.margin_small)
                            )
                            .clickable { navigateToUnitTask(taskListItem) },
                        text = taskListItem,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (isEditing) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_large),
                                end = dimensionResource(id = R.dimen.margin_normal),
                                top = dimensionResource(id = R.dimen.margin_small)
                            )
                            .clickable { navigateToUnitTask("") },
                        text = stringResource(id = R.string.add_edit_home_unit_task_list_add_new_task),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            //endregion
        }
    }

    uiState.alertDialog?.let {
        SmartAlertDialog(
            model = it,
            onOkClick = {}, // it is handled in SmartAlertDialogModel
            onDismissClick = closeAlertDialog
        )
    }

    val sheetState = rememberModalBottomSheetState()
    uiState.listBottomSheet?.let {
        SmartListBottomSheet(
            model = it,
            sheetState = sheetState,
            onDismissClick = closeListBottomSheet
        )
    }

    if (uiState.showProgress) {
        Box(
            modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun GeneralHomeUnitValue(
    homeUnitValue: HomeUnitScreenValueUiState<out Any>,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
    ) {

        // region Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth(),
                        text = stringResource(id = R.string.add_edit_home_unit_text_value),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_small)
                            ), text = homeUnitValue.value.toString()
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    text = getLastUpdateTime(
                        context = context,
                        lastUpdateTime = homeUnitValue.lastUpdateTime
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (homeUnitValue is HomeUnitScreenValueUiState.ActuatorValueUiState) {
                Switch(
                    modifier =
                        Modifier.padding(start = dimensionResource(id = R.dimen.margin_small)),
                    checked = homeUnitValue.value ?: false,
                    onCheckedChange = homeUnitValue.setValueFromSwitch
                )
            }
        }

        // endregion

        when (homeUnitValue) {
            // Min/Max Values
            is HomeUnitScreenValueUiState.NumberedValueUiState -> {
                Spacer(Modifier.size(dimensionResource(id = R.dimen.margin_small)))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1.0f)) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = stringResource(id = R.string.add_edit_home_unit_text_min_value),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = homeUnitValue.minValue.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = dimensionResource(id = R.dimen.margin_normal)),
                            text = getLastUpdateTime(
                                context = context,
                                homeUnitValue.minLastUpdateTime
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = homeUnitValue.clearMinValue) {
                        Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1.0f)) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = stringResource(id = R.string.add_edit_home_unit_text_max_value),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = homeUnitValue.maxValue.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = dimensionResource(id = R.dimen.margin_normal)),
                            text = getLastUpdateTime(
                                context = context,
                                homeUnitValue.maxLastUpdateTime
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = homeUnitValue.clearMaxValue) {
                        Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                    }
                }
            }

            is HomeUnitScreenValueUiState.LightSwitchValueUiState -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth(),
                        text = stringResource(id = R.string.add_edit_home_unit_text_second_value),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_small)
                            ), text = homeUnitValue.switchValue.toString()
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    text = getLastUpdateTime(
                        context = context,
                        homeUnitValue.switchLastUpdateTime
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is HomeUnitScreenValueUiState.WaterCirculationValueUiState -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth(),
                        text = stringResource(id = R.string.add_edit_home_unit_text_motion_value),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_small)
                            ), text = homeUnitValue.motionValue.toString()
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    text = getLastUpdateTime(
                        context = context,
                        homeUnitValue.motionLastUpdateTime
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.size(dimensionResource(id = R.dimen.margin_small)))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth(),
                        text = stringResource(id = R.string.add_edit_home_unit_text_temperature_value),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_small)
                            ), text = homeUnitValue.temperatureValue.toString()
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    text = getLastUpdateTime(
                        context = context,
                        homeUnitValue.temperatureLastUpdateTime
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1.0f)) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = stringResource(id = R.string.add_edit_home_unit_text_min_value),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = homeUnitValue.temperatureMinValue.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = dimensionResource(id = R.dimen.margin_normal)),
                            text = getLastUpdateTime(
                                context = context,
                                homeUnitValue.temperatureMinLastUpdateTime
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = homeUnitValue.clearTemperatureMinValue) {
                        Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1.0f)) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = stringResource(id = R.string.add_edit_home_unit_text_max_value),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = dimensionResource(id = R.dimen.margin_small)),
                                text = homeUnitValue.temperatureMaxValue.toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = dimensionResource(id = R.dimen.margin_normal)),
                            text = getLastUpdateTime(
                                context = context,
                                homeUnitValue.temperatureMaxLastUpdateTime
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = homeUnitValue.clearTemperatureMaxValue) {
                        Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                    }
                }
            }

            is HomeUnitScreenValueUiState.WatchDogValueUiState -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth(),
                        text = stringResource(id = R.string.add_edit_home_unit_text_input_value),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                start = dimensionResource(id = R.dimen.margin_small)
                            ), text = homeUnitValue.inputValue.toString()
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    text = getLastUpdateTime(
                        context = context,
                        homeUnitValue.inputLastUpdateTime
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            else -> {
                // nothing here
            }
        }
        HorizontalDivider(Modifier.padding(vertical = dimensionResource(id = R.dimen.margin_small)))
    }
}

@Composable
private fun HomeUnitHWUnits(
    hwUnitState: HomeUnitScreenHwUnitUiState,
    isEditing: Boolean
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
        text = stringResource(id = R.string.add_edit_home_unit_text_hw_unit_title),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.margin_large),
                end = dimensionResource(id = R.dimen.margin_normal)
            )
            .clickable(enabled = isEditing) {
                hwUnitState.hwUnitName.editAction()
            },
        text = hwUnitState.hwUnitName.value ?: "N/A"
    )
    when (hwUnitState) {
        is HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState -> {
            // nothing to add here
        }

        is HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState -> {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                text = stringResource(id = R.string.add_edit_home_unit_text_second_hw_unit_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.margin_large),
                        end = dimensionResource(id = R.dimen.margin_normal)
                    )
                    .clickable(enabled = isEditing) {
                        hwUnitState.switchHwUnitName.editAction()
                    },
                text = hwUnitState.switchHwUnitName.value ?: "N/A"
            )
        }

        is HomeUnitScreenHwUnitUiState.WatchDogHwUnitUiState -> {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                text = stringResource(id = R.string.add_edit_home_unit_text_input_hw_unit_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.margin_large),
                        end = dimensionResource(id = R.dimen.margin_normal)
                    )
                    .clickable(enabled = isEditing) {
                        hwUnitState.inputHwUnitName.editAction()
                    },
                text = hwUnitState.inputHwUnitName.value ?: "N/A"
            )
        }

        is HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState -> {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                text = stringResource(id = R.string.add_edit_home_unit_text_motion_hw_unit_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.margin_large),
                        end = dimensionResource(id = R.dimen.margin_normal)
                    )
                    .clickable(enabled = isEditing) {
                        hwUnitState.motionHwUnitName.editAction()
                    },
                text = hwUnitState.motionHwUnitName.value ?: "N/A"
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                text = stringResource(id = R.string.add_edit_home_unit_text_temperature_hw_unit_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.margin_large),
                        end = dimensionResource(id = R.dimen.margin_normal)
                    )
                    .clickable(enabled = isEditing) {
                        hwUnitState.temperatureHwUnitName.editAction()
                    },
                text = hwUnitState.temperatureHwUnitName.value ?: "N/A"
            )
        }
    }
}

@Composable
private fun HomeUnitAdditionalSettings(
    additionalSettings: HomeUnitScreenAdditionalSettingsUiState,
) {
    Spacer(Modifier.size(dimensionResource(id = R.dimen.margin_small)))
    when (additionalSettings) {

        is HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState -> {
            Row(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(),
                    text = stringResource(id = R.string.add_edit_home_unit_text_circulation_duration_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small)
                        ), text = getDayTime(additionalSettings.circulationDuration)
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(),
                    text = stringResource(id = R.string.unit_task_text_threshold_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small)
                        ), text = additionalSettings.temperatureThreshold.toString()
                )
            }
        }

        is HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState -> {
            Row(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(),
                    text = stringResource(id = R.string.add_edit_home_unit_text_watch_dog_delay_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small)
                        ), text = getDayTime(additionalSettings.watchDogDelay)
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.margin_normal)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(),
                    text = stringResource(id = R.string.add_edit_home_unit_text_watch_dog_timeout_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_small)
                        ), text = getDayTime(additionalSettings.watchDogTimeout)
                )
            }
        }
    }
}

// region previews

// region Number Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview1() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                showProgress = false,
                unitName = "Home Unit Name",
                value = HomeUnitScreenValueUiState.NumberedValueUiState(
                    value = 23.5,
                    lastUpdateTime = System.currentTimeMillis(),
                    minValue = 19.0,
                    minLastUpdateTime = System.currentTimeMillis() - 100000,
                    maxValue = 28.0,
                    maxLastUpdateTime = System.currentTimeMillis() - 200000,
                    {}, {}
                ),
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState(Editable("HW Unit Name") {}),
                additionalSettings = null,
                firebaseNotify = true,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_BOOLEAN_APPLY,
                unitTasks = listOf("Auto Off", "Notify on High Temp")
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// region Switch Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview2() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                false,
                unitName = "Actuator",
                value = HomeUnitScreenValueUiState.SwitchValueUiState(
                    value = false,
                    lastUpdateTime = System.currentTimeMillis() - 100000,
                    setValueFromSwitch = {}
                ),
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState(Editable("HW Unit Name") {}),
                additionalSettings = null,
                firebaseNotify = true,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_HW_UNIT,
                unitTasks = emptyList()
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// region LightSwitch Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview3() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                false,
                unitName = "Light Switch",
                value = HomeUnitScreenValueUiState.LightSwitchValueUiState(
                    value = false,
                    lastUpdateTime = System.currentTimeMillis() - 100000,
                    setValueFromSwitch = {},
                    switchValue = true,
                    switchLastUpdateTime = System.currentTimeMillis() - 300000
                ),
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.LightSwitchHwUnitUiState(
                    Editable("HW Unit Name") {},
                    Editable("Switch HW Unit Name") {}
                ),
                additionalSettings = null,
                firebaseNotify = true,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_HW_UNIT,
                unitTasks = emptyList()
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// region WaterCirculation Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview4() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                false,
                unitName = "Water Circulation",
                value = HomeUnitScreenValueUiState.WaterCirculationValueUiState(
                    value = false,
                    lastUpdateTime = System.currentTimeMillis(),
                    setValueFromSwitch = {},
                    motionValue = true,
                    motionLastUpdateTime = System.currentTimeMillis() - 300000,
                    temperatureValue = 23.5,
                    temperatureLastUpdateTime = System.currentTimeMillis() - 100000,
                    temperatureMinValue = 19.0,
                    temperatureMinLastUpdateTime = System.currentTimeMillis() - 1000000,
                    temperatureMaxValue = 28.0,
                    temperatureMaxLastUpdateTime = System.currentTimeMillis() - 200000,
                    {}, {}
                ),
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.WaterCirculationHwUnitUiState(
                    Editable("HW Unit Name") {},
                    Editable("Motion HW Unit Name") {},
                    Editable("Temperature HW Unit Name") {}
                ),
                additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WaterCirculationAdditionalSettingsUiState(
                    circulationDuration = 120000L,
                    temperatureThreshold = 30.0f
                ),
                firebaseNotify = true,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_HW_UNIT,
                unitTasks = emptyList()
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// region WatchDog Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview5() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                false,
                unitName = "Watch Dog",
                value = HomeUnitScreenValueUiState.WatchDogValueUiState(
                    value = true,
                    lastUpdateTime = System.currentTimeMillis() - 100000,
                    setValueFromSwitch = {},
                    inputValue = true,
                    inputLastUpdateTime = System.currentTimeMillis() - 100000
                ),
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.WatchDogHwUnitUiState(
                    Editable("HW Unit Name") {},
                    Editable("Switch HW Unit Name") {}
                ),
                additionalSettings = HomeUnitScreenAdditionalSettingsUiState.WatchDogAdditionalSettingsUiState(
                    watchDogDelay = 60000L,
                    watchDogTimeout = 300000L
                ),
                firebaseNotify = true,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_HW_UNIT,
                unitTasks = emptyList()
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// region Empty Value preview
@Preview
@Composable
private fun HomeUnitScreenImplPreview42() {
    MaterialTheme {
        HomeUnitScreenImpl(
            snackBarHostState = SnackbarHostState(),
            HomeUnitScreenUiState(
                false,
                unitName = "Home Unit Name",
                value = null,
                unitType = Editable("temperatures") {},
                roomName = Editable("Living Room") {},
                hwUnit = HomeUnitScreenHwUnitUiState.GeneralHwUnitUiState(Editable(null) {}),
                additionalSettings = null,
                firebaseNotify = false,
                firebaseNotifyTrigger = Editable(RISING_EDGE) {},
                showInTaskList = false,
                lastTriggerSource = LAST_TRIGGER_SOURCE_HW_UNIT,
                unitTasks = emptyList()
            ), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
// endregion

// endregion