package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.RoomDetailTopAppBar
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.ROOM_NAME_ARG
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartGraphs.ROOM_LIST_GRAPH_ROOT
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomlist.RoomListScreen
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.tasklist.TaskListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SmartNavGraph(
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    navActions: SmartNavigationActions,
    drawerState: DrawerState,
    startDestination: String,
    modifier: Modifier = Modifier,
) {

    // to be able to share SmartDrawerViewModel between screens
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

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
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    RoomListScreen(openDrawer = { coroutineScope.launch { drawerState.open() } },
                        onAddNewRoom = {},
                        onRoomClick = {
                            navActions.navigateToRoomDetail(it)
                        })
                }
            }
            composable(SmartDestinations.ROOM_DETAIL_ROUTE) { backStackEntry ->
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    val roomName = backStackEntry.arguments?.getString(ROOM_NAME_ARG) ?: "null"
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
                //nestedGraphs()
            }

            composable(SmartDestinations.TAK_LIST_ROUTE) {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    TaskListScreen(openDrawer = { coroutineScope.launch { drawerState.open() } },
                        onAddNewHomeUnit = {},
                        onTaskClick = { homeUnitType, homeUnitName ->
                            navActions.navigateToRoomDetail(homeUnitName)
                        })
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
