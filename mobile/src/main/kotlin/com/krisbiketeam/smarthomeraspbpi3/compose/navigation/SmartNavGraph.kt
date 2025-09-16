package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import android.app.Activity
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.HOME_UNIT_TYPE
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.HW_UNIT_NAME
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.ROOM_NAME_ARG
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartGraphs.ROOM_LIST_GRAPH_ROOT
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartGraphs.TASK_LIST_GRAPH_ROOT
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.logschart.LogsChartScreen
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomdetails.RoomDetailScreen
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomlist.RoomListScreen
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.settings.SettingsScreen
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.tasklist.TaskListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SmartNavGraph(
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    navActions: SmartNavigationActions,
    drawerState: DrawerState,
    drawerGesturesEnabled: (Boolean) -> Unit,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        navigation(
            route = ROOM_LIST_GRAPH_ROOT,
            startDestination = SmartDestinations.ROOM_LIST_ROUTE,
        ) {
            composable(SmartDestinations.ROOM_LIST_ROUTE) {
                drawerGesturesEnabled(true)
                RoomListScreen(openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onAddNewRoom = {},
                    onRoomClick = {
                        navActions.navigateToRoomDetail(it)
                    })
            }
            composable(SmartDestinations.ROOM_DETAIL_ROUTE) { backStackEntry ->
                drawerGesturesEnabled(true)
                RoomDetailScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onHomeUnitClick = { homeUnitType, homeUnitName ->
                        navActions.navigateToHomeUnitDetail(homeUnitType, homeUnitName)
                    },
                    onNewHomeUnitClick = { roomName ->
                        //navActions.navigateToToHomeUnitTypeChooserDialogFragment(roomName)
                    },
                    showLogs = { hwUnitName, homeUnitType ->
                        navActions.navigateToLogsChart(hwUnitName to homeUnitType)
                    },
                    roomName = backStackEntry.arguments?.getString(ROOM_NAME_ARG) ?: "null",
                    navigateUp = {
                        navActions.navigateUp()
                    }
                )
            }
        }

        navigation(
            route = TASK_LIST_GRAPH_ROOT,
            startDestination = SmartDestinations.TAK_LIST_ROUTE,
        ) {
            composable(SmartDestinations.TAK_LIST_ROUTE) {
                drawerGesturesEnabled(true)
                TaskListScreen(openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onAddNewHomeUnit = {},
                    onTaskClick = { homeUnitType, homeUnitName ->
                        // TODO navigate to proper HomeUnit Detail screen
                        navActions.navigateToRoomDetail(homeUnitName)
                    })
            }
        }

        composable(
            SmartDestinations.LOGS_CHART_ROUTE,
            arguments = listOf(navArgument(HW_UNIT_NAME) { type = NavType.StringType },
                navArgument(HW_UNIT_NAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(HOME_UNIT_TYPE) {
                    type = NavType.StringType
                    nullable = true
                })
        ) { backStackEntry ->

            val homeUnitType = backStackEntry.arguments?.getString(HOME_UNIT_TYPE)
            val hwUnitName = backStackEntry.arguments?.getString(HW_UNIT_NAME)

            drawerGesturesEnabled(false)
            LogsChartScreen(
                openDrawer = { coroutineScope.launch { drawerState.open() } },
                preselection = if (homeUnitType != null && hwUnitName != null) hwUnitName to homeUnitType else null
            )
        }

        composable(
            SmartDestinations.SETTINGS_ROUTE
        ) {
            SettingsScreen(
                navigateUp = { navActions.navigateUp() },
                navigateToLogin = {},
                navigateToHomeSetup = {},
                navigateToHwUnitList = {},
                navigateToHwUnitErrorList = {},
                navigateToLogsList = {},
                navigateToHwUnitErrorLogsList = {})
        }
    }
}

// Keys for navigation
const val ADD_EDIT_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val DELETE_RESULT_OK = Activity.RESULT_FIRST_USER + 2
const val EDIT_RESULT_OK = Activity.RESULT_FIRST_USER + 3
