/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krisbiketeam.smarthomeraspbpi3.compose.screens.tasklist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.compose.components.grid.SmartStaggeredGrid
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.TaskListTopAppBar
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun TaskListScreen(
    openDrawer: () -> Unit,
    onAddNewHomeUnit: () -> Unit,
    onTaskClick: (HomeUnitType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListScreenViewModel = koinViewModel(),
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TaskListTopAppBar(
                openDrawer = openDrawer,
                isEditing,
                onEditClicked = { isEditing = true },
                onFinishClicked = { isEditing = false }
            )
        },
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (isEditing) {
                FloatingActionButton(onClick = onAddNewHomeUnit) {
                    Icon(Icons.Filled.Add, stringResource(id = R.string.menu_add))
                }
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.roomListFlow.collectAsStateWithLifecycle(emptyList())

        SmartStaggeredGrid(
            uiState,
            { model ->
                model.switchUnit?.let { (homeUnitType, homeUnitName) ->
                    onTaskClick(
                        homeUnitType,
                        homeUnitName
                    )
                }
            },
            { model, isChecked ->
                Timber.d("OnCheckedChangeListener isChecked: $isChecked item: $model")
                model.switchUnit?.let { (homeUnitType, homeUnitName) ->
                    viewModel.switchHomeUnitState(
                        homeUnitType,
                        homeUnitName,
                        isChecked
                    )
                }
            },
            Modifier.padding(paddingValues)
        )
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