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

package com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation.SmartScreens.ROOM_DETAIL_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation.SmartScreens.ROOM_LIST_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation.SmartScreens.TASK_LIST_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation.SmartDestinationsArgs.ROOM_NAME_ARG
import com.krisbiketeam.smarthomeraspbpi3.SmartActivity

/**
 * Screens used in [SmartDestinations]
 */
object SmartScreens {
    const val ROOM_LIST_SCREEN = "roomList"
    const val TASK_LIST_SCREEN = "taskList"
    const val ROOM_DETAIL_SCREEN = "room"
}

/**
 * Arguments used in [SmartDestinations] routes
 */
object SmartDestinationsArgs {
    const val ROOM_NAME_ARG = "roomName"
    const val USER_MESSAGE_ARG = "userMessage"
    const val TITLE_ARG = "title"
}

/**
 * Destinations used in the [SmartActivity]
 */
object SmartDestinations {
    const val ROOM_LIST_ROUTE = ROOM_LIST_SCREEN
    const val TAK_LIST_ROUTE = TASK_LIST_SCREEN
    const val ROOM_DETAIL_ROUTE = "$ROOM_DETAIL_SCREEN/{$ROOM_NAME_ARG}"
    //const val ADD_EDIT_TASK_ROUTE = "$ADD_EDIT_TASK_SCREEN/{$TITLE_ARG}?$TASK_ID_ARG={$TASK_ID_ARG}"
}

/**
 * Models the navigation actions in the app.
 */
class SmartNavigationActions(private val navController: NavHostController) {

    fun navigateToRoomList() {
        navController.navigate(SmartDestinations.ROOM_LIST_ROUTE) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    fun navigateToTaskList() {
        navController.navigate(SmartDestinations.TAK_LIST_ROUTE) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    fun navigateToRoomDetail(roomName: String) {
        navController.navigate("$ROOM_DETAIL_SCREEN/$roomName") {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }
}
