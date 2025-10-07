package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import android.app.Activity
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.homeunit.HomeUnitScreen
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
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        navigation<RoomListGraph>(
            startDestination = RoomList,
        ) {
            composable<RoomList> {
                drawerGesturesEnabled(true)
                RoomListScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onAddNewRoom = {},
                    onRoomClick = {
                        navActions.navigateToRoomDetail(RoomRoute(it))
                    })
            }
            composable<RoomRoute> { backStackEntry ->
                drawerGesturesEnabled(true)
                val roomRoute: RoomRoute = backStackEntry.toRoute()
                RoomDetailScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onHomeUnitClick = navActions::navigateToHomeUnitDetail,
                    onNewHomeUnitClick = { roomName ->
                        //navActions.navigateToToHomeUnitTypeChooserDialogFragment(roomName)
                    },
                    showLogs = { hwUnitName, homeUnitType ->
                        navActions.navigateToLogsChart(
                            LogsChartRoute(
                                homeUnitType.firebaseTableName,
                                hwUnitName
                            )
                        )
                    },
                    roomName = roomRoute.name,
                    navigateUp = {
                        navActions.navigateUp()
                    }
                )
            }
            composable<HomeUnitRoute> { backStackEntry ->
                drawerGesturesEnabled(false)
                val homeUnitRoute: HomeUnitRoute = backStackEntry.toRoute()
                HomeUnitScreen(
                    homeUnitRoute.homeUnitType,
                    homeUnitRoute.homeUnitName,
                    navigateUp = {
                        navActions.navigateUp()
                    }
                )
            }
        }

        navigation<TaskListGraph>(
            startDestination = TaskList
        ) {
            composable<TaskList> {
                drawerGesturesEnabled(true)
                TaskListScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onAddNewHomeUnit = {},
                    onTaskClick = navActions::navigateToHomeUnitDetail
                )
            }
        }

        composable<LogsChartRoute> { backStackEntry ->
            val logsChartRoute: LogsChartRoute = backStackEntry.toRoute()

            val homeUnitType = logsChartRoute.homeUnitType
            val hwUnitName = logsChartRoute.hwUnitName

            drawerGesturesEnabled(false)
            LogsChartScreen(
                openDrawer = { coroutineScope.launch { drawerState.open() } },
                preselection = if (homeUnitType != null && hwUnitName != null) hwUnitName to homeUnitType else null
            )
        }

        composable<Settings> {
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
