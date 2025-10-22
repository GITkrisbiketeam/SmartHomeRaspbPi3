package com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog.SmartAlertDialog
import com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit.HomeUnitCard
import com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit.HomeUnitCardModel
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.RoomDetailTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.HomeUnitRoute
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoomDetailScreen(
    openDrawer: () -> Unit,
    onHomeUnitClick: (HomeUnitRoute) -> Unit,
    onNewHomeUnitClick: (String) -> Unit,
    showLogs: (String, HomeUnitType) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    roomName: String,
    viewModel: RoomDetailScreenViewModel = koinViewModel { parametersOf(roomName) },
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            RoomDetailTopAppBar(openDrawer = openDrawer,
                title = roomName,
                isEditing = isEditing,
                onEditClicked = { isEditing = true },
                onSave = {
                    viewModel.actionSave()?.let {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(it))
                        }
                    }
                },
                onDiscard = {
                    if (viewModel.actionDiscard()) {
                        isEditing = false
                    }
                },
                onDelete = {
                    viewModel.actionDeleteRoom()
                })
        },
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (isEditing) {
                FloatingActionButton(onClick = { onNewHomeUnitClick(roomName) }) {
                    Icon(Icons.Filled.Add, stringResource(id = R.string.menu_add))
                }
            }
        }) { paddingValues ->
        val uiState by viewModel.homeUnitsList.collectAsStateWithLifecycle(emptyList())

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isEditing) {
                var text by rememberSaveable { mutableStateOf(roomName) }

                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.margin_normal)),
                    value = text,
                    onValueChange = {
                        text = it
                        viewModel.setRoomName(it)
                    })
            }
            HomeUnitList(
                modifier = modifier
                    .fillMaxSize(),
                homeUnits = uiState,
                onHomeUnitClick = {
                    onHomeUnitClick(
                        HomeUnitRoute(
                            roomName = roomName,
                            homeUnitType = it.first.firebaseTableName,
                            homeUnitName = it.second
                        )
                    )
                },
                showLogs = { showLogs(it.first, it.second) },
                switchHomeUnitState = { homeUnit, switchState ->
                    viewModel.switchHomeUnitState(
                        homeUnit, switchState
                    )
                }
            )
        }
    }
    val alertDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    alertDialog?.let {
        SmartAlertDialog(
            model = it,
            onOkClick = {
                viewModel.showDialog.value = null
                isEditing = false
            }, onDismissClick = {
                viewModel.showDialog.value = null
            })
    }

    val navigateUp by viewModel.navigateUp.collectAsStateWithLifecycle()
    if (navigateUp) {
        navigateUp()
    }

    val showProgress by viewModel.showProgress.collectAsStateWithLifecycle()
    if (showProgress) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
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
private fun HomeUnitList(
    modifier: Modifier = Modifier,
    homeUnits: List<HomeUnitCardModel>,
    onHomeUnitClick: (Pair<HomeUnitType, String>) -> Unit,
    switchHomeUnitState: (Pair<HomeUnitType, String>, Boolean) -> Unit,
    showLogs: (Pair<String, HomeUnitType>) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        content = {
            items(homeUnits) { homeUnitModel ->
                HomeUnitCard(
                    homeUnitModel,
                    onClick = remember(homeUnitModel) { { onHomeUnitClick(homeUnitModel.id.homeUnitType to homeUnitModel.id.homeUnitName) } },
                    showLogs = remember(homeUnitModel) {
                        {
                            homeUnitModel.id.hwUnitName?.let { hwUnitName ->
                                showLogs(
                                    hwUnitName to homeUnitModel.id.homeUnitType
                                )
                            } ?: Unit
                        }
                    },
                    onSwitch = remember {
                        {
                            switchHomeUnitState(
                                homeUnitModel.id.homeUnitType to homeUnitModel.id.homeUnitName,
                                it
                            )
                        }
                    })
            }
        },
        modifier = modifier.fillMaxSize()
    )
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