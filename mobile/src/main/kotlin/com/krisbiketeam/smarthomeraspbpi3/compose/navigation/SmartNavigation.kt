package com.krisbiketeam.smarthomeraspbpi3.compose.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.krisbiketeam.smarthomeraspbpi3.R
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data object RoomListGraph
@Serializable
data object TaskListGraph

@Serializable
data object RoomList
@Serializable
data class RoomRoute(val name: String)

@Serializable
data object TaskList
@Serializable
data class HomeUnitRoute(val roomName: String? = null, val homeUnitType: String, val homeUnitName: String)

@Serializable
data class LogsChartRoute(val hwUnitName: String? = null, val homeUnitType: String? = null)

@Serializable
data object Settings

data class SmartTopLevelRoute<T : Any>(val route: T,
                                       @DrawableRes val icon: Int,
                                       @StringRes val titleTextId: Int)

val smartTopLevelRoutes = listOf(
    SmartTopLevelRoute(
        route = RoomListGraph,
        icon = R.drawable.ic_baseline_other_houses_24,
        titleTextId = R.string.room_list_title),
    SmartTopLevelRoute(
        route = TaskListGraph,
        icon = R.drawable.ic_baseline_view_headline_24,
        titleTextId = R.string.task_list_title),
    SmartTopLevelRoute(
        route = LogsChartRoute(),
        icon = R.drawable.ic_statistics,
        titleTextId = R.string.logs_title)
)

fun NavDestination?.isTopLevelDestinationInHierarchy(smartTopLevelRoute: SmartTopLevelRoute<*>): Boolean {
    Timber.v(
        "isTopLevelDestinationInHierarchy smartTopLevelRoute:$smartTopLevelRoute\n" +
                "currentDestination:$this\n" +
                "currentDestination hierarchy:\n    ${this?.hierarchy?.joinToString("\n    ")}"
    )
    return this?.hierarchy?.any { it.hasRoute(smartTopLevelRoute.route::class) } == true
}

/**
 * Models the navigation actions in the app.
 */
class SmartNavigationActions(private val navController: NavHostController) {

    fun navigateToRoomList(resetState: Boolean = false) {
        navController.navigate(RoomList) {
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
            restoreState = !resetState
        }
    }

    fun navigateToTaskList() {
        navController.navigate(TaskListGraph) {
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

    fun navigateToSettings() {
        Timber.e("navigateToSettings")
        navController.navigate(Settings)
    }

    fun navigateToLogsChart(logsChartRoute: LogsChartRoute) {
        navController.navigate(logsChartRoute) {
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

    fun navigateToRoomDetail(roomRoute: RoomRoute) {
        Timber.e("navigateToRoomDetail $roomRoute")
        navController.navigate(roomRoute) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id)
        }
    }

    fun navigateToHomeUnitDetail(homeUnitRoute: HomeUnitRoute) {
        Timber.e("navigateToHomeUnitDetail $homeUnitRoute")
        navController.navigate(homeUnitRoute)
    }

    fun navigateUp() {
        navController.navigateUp()
    }

    fun navigateToSmartTopLevelDestination(smartTopLevelRoute: SmartTopLevelRoute<*>) {
        Timber.d("Navigation: $smartTopLevelRoute")

        when (smartTopLevelRoute.route) {
            is RoomListGraph -> navigateToRoomList()
            is TaskListGraph -> navigateToTaskList()
            is LogsChartRoute -> navigateToLogsChart(LogsChartRoute())
            else -> Unit
        }
    }
}
