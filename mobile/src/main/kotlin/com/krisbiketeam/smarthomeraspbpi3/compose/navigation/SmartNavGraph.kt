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

package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krisbiketeam.smarthomeraspbpi3.compose.core.drawer.SmartModalDrawer
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.RoomDetailTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.TaskListTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.ROOM_NAME_ARG
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomlist.RoomListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SmartNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    startDestination: String = SmartDestinations.ROOM_LIST_ROUTE,
    navActions: SmartNavigationActions = remember(navController) {
        SmartNavigationActions(navController)
    }
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination

    // to be able to share SmartDrawerViewModel between screens
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SmartDestinations.ROOM_LIST_ROUTE) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                SmartModalDrawer(drawerState, currentRoute, navActions) {
                    RoomListScreen(openDrawer = { coroutineScope.launch { drawerState.open() } },
                        onAddNewRoom = {},
                        onRoomClick = {
                            navActions.navigateToRoomDetail(it)
                        })
                }
            }
        }
        composable(SmartDestinations.TAK_LIST_ROUTE) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                SmartModalDrawer(drawerState, currentRoute, navActions) {
                    // A surface container using the 'background' color from the theme
                    Scaffold(
                        topBar = {
                            TaskListTopAppBar(
                                openDrawer = { coroutineScope.launch { drawerState.open() } },
                                false,
                                onEditClicked = { },
                                onFinishClicked = {  }
                            )
                        },
                        modifier = modifier.fillMaxSize(),

                    ) { paddingValues ->
                        Text(
                            text = "Hello Android!",
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
        composable(SmartDestinations.ROOM_DETAIL_ROUTE) { backStackEntry ->
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                val roomName = backStackEntry.arguments?.getString(ROOM_NAME_ARG)?:"null"
                SmartModalDrawer(drawerState, currentRoute, navActions, roomName) {
                    // A surface container using the 'background' color from the theme
                    Scaffold(
                        topBar = {
                            RoomDetailTopAppBar(
                                openDrawer = { coroutineScope.launch { drawerState.open() } },
                                isEditing = false,
                                onEditClicked = { /*TODO*/ },
                                onDone = { /*TODO*/ },
                                onDiscard = { /*TODO*/ }) {

                            }
                        },
                        modifier = modifier.fillMaxSize(),

                        ) { paddingValues ->
                        Text(
                            text = roomName,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
            /*TaskDetailScreen(
                onEditTask = { taskId ->
                    navActions.navigateToAddEditTask(R.string.edit_task, taskId)
                },
                onBack = { navController.popBackStack() },
                onDeleteTask = { navActions.navigateToTasks(DELETE_RESULT_OK) }
            )*/
        }
    }
}

// Keys for navigation
const val ADD_EDIT_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val DELETE_RESULT_OK = Activity.RESULT_FIRST_USER + 2
const val EDIT_RESULT_OK = Activity.RESULT_FIRST_USER + 3
