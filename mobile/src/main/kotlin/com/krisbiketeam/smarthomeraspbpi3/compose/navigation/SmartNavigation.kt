package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.SmartActivity
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.HOME_UNIT_TYPE
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.HW_UNIT_NAME
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs.ROOM_NAME_ARG
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartScreens.LOGS_CHART_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartScreens.ROOM_DETAIL_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartScreens.ROOM_LIST_SCREEN
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartScreens.TASK_LIST_SCREEN
import timber.log.Timber

/**
 * Screens used in [SmartDestinations]
 */
object SmartGraphs {
    const val ROOM_LIST_GRAPH_ROOT = "roomListGraphRoot"
    const val TASK_LIST_GRAPH_ROOT = "taskListGraphRoot"
}

object SmartScreens {
    const val ROOM_LIST_SCREEN = "roomList"
    const val TASK_LIST_SCREEN = "taskList"
    const val LOGS_CHART_SCREEN = "logsChart"
    const val ROOM_DETAIL_SCREEN = "roomDetail"
}

/**
 * Arguments used in [SmartDestinations] routes
 */
object SmartDestinationsArgs {
    const val ROOM_NAME_ARG = "roomName"
    const val HOME_UNIT_TYPE = "homeUnitType"
    const val HW_UNIT_NAME = "hwUnitName"
    const val TITLE_ARG = "title"
}

/**
 * Destinations used in the [SmartActivity]
 */
object SmartDestinations {
    const val ROOM_LIST_ROUTE = ROOM_LIST_SCREEN
    const val TAK_LIST_ROUTE = TASK_LIST_SCREEN
    const val LOGS_CHART_ROUTE =
        "$LOGS_CHART_SCREEN?$HW_UNIT_NAME={$HW_UNIT_NAME}?$HOME_UNIT_TYPE={$HOME_UNIT_TYPE}"
    const val ROOM_DETAIL_ROUTE = "$ROOM_DETAIL_SCREEN/{$ROOM_NAME_ARG}"
    //const val ADD_EDIT_TASK_ROUTE = "$ADD_EDIT_TASK_SCREEN/{$TITLE_ARG}?$TASK_ID_ARG={$TASK_ID_ARG}"
}

enum class SmartTopLevelDestination(
    val destination: String,
    @DrawableRes val icon: Int,
    @StringRes val titleTextId: Int,
) {
    ROOM_LIST_ROUTE(
        destination = SmartGraphs.ROOM_LIST_GRAPH_ROOT,
        icon = R.drawable.ic_baseline_other_houses_24,
        titleTextId = R.string.room_list_title,
    ),
    TAK_LIST_ROUTE(
        destination = SmartGraphs.TASK_LIST_GRAPH_ROOT,
        icon = R.drawable.ic_baseline_view_headline_24,
        titleTextId = R.string.task_list_title,
    ),
    LOGS_CHART_ROUTE(
        destination = SmartDestinations.LOGS_CHART_ROUTE,
        icon = R.drawable.ic_statistics,
        titleTextId = R.string.logs_title,
    ),
}

fun NavDestination?.isTopLevelDestinationInHierarchy(destination: SmartTopLevelDestination): Boolean {
    Timber.v(
        "isTopLevelDestinationInHierarchy destination:${destination.destination}\n" +
                "currentDestination:$this\n" +
                "currentDestination hierarchy:\n    ${this?.hierarchy?.joinToString("\n    ")}"
    )
    return this?.hierarchy?.any {
        it.route?.contains(destination.destination, true) ?: false
    } ?: false
}

fun NavDestination?.isSelectedDestination(destination: String) =
    this?.route == destination


/**
 * Models the navigation actions in the app.
 */
class SmartNavigationActions(private val navController: NavHostController) {

    fun navigateToRoomList() {
        navController.navigate(SmartGraphs.ROOM_LIST_GRAPH_ROOT) {
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
        navController.navigate(SmartGraphs.TASK_LIST_GRAPH_ROOT) {
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

    fun navigateToLogsChart(preselection: Pair<String, HomeUnitType>? = null) {
        val route = if (preselection != null) {
            "$LOGS_CHART_SCREEN?$HW_UNIT_NAME=${preselection.first}?$HOME_UNIT_TYPE=${preselection.second.firebaseTableName}"
        } else {
            SmartDestinations.LOGS_CHART_ROUTE
        }
        navController.navigate(route) {
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
        Timber.e("navigateToRoomDetail $roomName")
        navController.navigate("$ROOM_DETAIL_SCREEN/$roomName") {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id)
        }
    }

    fun navigateToHomeUnitDetail(homeUnitName: HomeUnitType, homeUnitType: String) {
        // TODO add proper ROOM_DETAIL_SCREEN
        navController.navigate("$ROOM_DETAIL_SCREEN/$homeUnitName") {
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

    fun navigateUp() {
        navController.navigateUp()
    }

    fun navigateToSmartTopLevelDestination(topLevelDestination: SmartTopLevelDestination) {
        Timber.d("Navigation: ${topLevelDestination.name}")

        when (topLevelDestination) {
            SmartTopLevelDestination.ROOM_LIST_ROUTE -> navigateToRoomList()
            SmartTopLevelDestination.TAK_LIST_ROUTE -> navigateToTaskList()
            SmartTopLevelDestination.LOGS_CHART_ROUTE -> navigateToLogsChart()
        }
    }
}
