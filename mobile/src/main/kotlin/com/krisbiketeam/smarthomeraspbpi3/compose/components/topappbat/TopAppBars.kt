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

package com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.krisbiketeam.smarthomeraspbpi3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListTopAppBar(
    openDrawer: () -> Unit,
    isEditing: Boolean,
    onEditClicked: () -> Unit,
    onFinishClicked: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.room_list_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
        },
        actions = {
            if (isEditing) {
                IconButton(onClick = onFinishClicked) {
                    Icon(Icons.Filled.Done, stringResource(id = R.string.menu_finish))
                }
            } else {
                MoreEditMenu(onEditClicked)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopAppBar(
    openDrawer: () -> Unit,
    isEditing: Boolean,
    onEditClicked: () -> Unit,
    onFinishClicked: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.task_list_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
        },
        actions = {
            if (isEditing) {
                IconButton(onClick = onFinishClicked) {
                    Icon(Icons.Filled.Done, stringResource(id = R.string.menu_finish))
                }
            } else {
                MoreEditMenu(onEditClicked)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTopAppBar(
    openDrawer: () -> Unit,
    onPickDateClicked: () -> Unit,
    onFilterLogsClicked: () -> Unit,
    onClearAll: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.logs_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
        },
        actions = {
            LogsFilterMenu(onPickDateClicked, onFilterLogsClicked, onClearAll)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailTopAppBar(
    openDrawer: () -> Unit,
    isEditing: Boolean,
    onEditClicked: () -> Unit,
    onDone: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.room_details_title))
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
        },

        actions = {
            if (isEditing) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Filled.Done, stringResource(id = R.string.menu_finish))
                }
                IconButton(onClick = onDiscard) {
                    Icon(Icons.Filled.Close, stringResource(id = R.string.menu_discard))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                }
            } else {
                MoreEditMenu(onEditClicked)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUnitDetailTopAppBar(
    onBack: () -> Unit,
    isEditing: Boolean,
    onEditClicked: () -> Unit,
    onDone: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.room_details_title))
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (isEditing) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Filled.Done, stringResource(id = R.string.menu_finish))
                }
                IconButton(onClick = onDiscard) {
                    Icon(Icons.Filled.Close, stringResource(id = R.string.menu_discard))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
                }
            } else {
                MoreEditMenu(onEditClicked)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskTopAppBar(@StringRes title: Int, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomeTopAppBar(
    openDrawer: () -> Unit,
    onFilterAllTasks: () -> Unit,
    onFilterActiveTasks: () -> Unit,
    onFilterCompletedTasks: () -> Unit,
    onClearCompletedTasks: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.room_list_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
        },
        actions = {
            LogsFilterMenu(onFilterAllTasks, onFilterActiveTasks, onFilterCompletedTasks)
            MoreEditMenu(onClearCompletedTasks)
        },
        modifier = Modifier.fillMaxWidth()
    )
}


// region private methods
@Composable
private fun LogsFilterMenu(
    onPickDateClicked: () -> Unit,
    onFilterLogsClicked: () -> Unit,
    onClearAll: () -> Unit,
) {
    TopAppBarDropdownMenu(
        iconContent = {
            Icon(
                painterResource(id = R.drawable.ic_baseline_view_headline_24),
                stringResource(id = R.string.menu_filter)
            )
        }
    ) { closeMenu ->
        DropdownMenuItem(onClick = { onPickDateClicked(); closeMenu() }, text = {
            Text(text = stringResource(id = R.string.menu_date_picker))
        })

        DropdownMenuItem(onClick = { onFilterLogsClicked(); closeMenu() }, text = {
            Text(text = stringResource(id = R.string.menu_filter))
        })

        DropdownMenuItem(onClick = { onClearAll(); closeMenu() }, text = {
            Text(text = stringResource(id = R.string.menu_clear_all))
        })
    }
}

@Composable
private fun MoreEditMenu(
    onEditClicked: () -> Unit,
) {
    TopAppBarDropdownMenu(
        iconContent = {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
    ) { closeMenu ->
        DropdownMenuItem(onClick = { onEditClicked(); closeMenu() }, text = {
            Text(text = stringResource(id = R.string.menu_edit))
        })
    }
}

@Composable
private fun TopAppBarDropdownMenu(
    iconContent: @Composable () -> Unit,
    content: @Composable ColumnScope.(() -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = !expanded }) {
            iconContent()
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            content { expanded = !expanded }
        }
    }
}

// endregion

// region preview
@Preview
@Composable
private fun RoomListTopAppBarPreview() {
    MaterialTheme {
        Surface {
            RoomListTopAppBar({}, false, {}, {})
        }
    }
}

@Preview
@Composable
private fun RoomListTopAppBarPreviewEditing() {
    MaterialTheme {
        Surface {
            RoomListTopAppBar({}, true, {}, {})
        }
    }
}

@Preview
@Composable
private fun TaskListTopAppBarPreview() {
    MaterialTheme {
        Surface {
            TaskListTopAppBar({}, false, {}, {})
        }
    }
}
@Preview
@Composable
private fun TaskListTopAppBarPreviewEditing() {
    MaterialTheme {
        Surface {
            TaskListTopAppBar({}, true, {}, {})
        }
    }
}

@Preview
@Composable
private fun LogsTopAppBarPreview() {
    MaterialTheme {
        Surface {
            LogsTopAppBar({ }, { }, {}, {})
        }
    }
}

@Preview
@Composable
private fun RoomDetailTopAppBarPreview() {
    MaterialTheme {
        Surface {
            RoomDetailTopAppBar({}, false, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
private fun RoomDetailTopAppBarPreviewEditing() {
    MaterialTheme {
        Surface {
            RoomDetailTopAppBar({}, true, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
private fun AddEditTaskTopAppBarPreview() {
    MaterialTheme {
        Surface {
            AddEditTaskTopAppBar(R.string.add_edit_hw_unit_name_title, { })
        }
    }
}


// endregion