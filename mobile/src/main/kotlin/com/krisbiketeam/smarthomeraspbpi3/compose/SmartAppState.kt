package com.krisbiketeam.smarthomeraspbpi3.compose

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinations.LOGS_ROUTE
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinations.ROOM_LIST_ROUTE
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinations.TAK_LIST_ROUTE
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartTopLevelDestination
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberSmartAppState(
    windowSizeClass: WindowSizeClass,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): SmartAppState {
    return remember(
        navController,
        coroutineScope,
        windowSizeClass,
    ) {
        SmartAppState(
            navController,
            coroutineScope,
            windowSizeClass,
            SmartNavigationActions(navController)
        )
    }
}

@Stable
class SmartAppState(
    private val navController: NavHostController,
    val coroutineScope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
    val navActions: SmartNavigationActions
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentSmartTopLevelDestination: SmartTopLevelDestination?
        @Composable get() = when (currentDestination?.route) {
            ROOM_LIST_ROUTE -> SmartTopLevelDestination.ROOM_LIST_ROUTE
            TAK_LIST_ROUTE -> SmartTopLevelDestination.TAK_LIST_ROUTE
            LOGS_ROUTE -> SmartTopLevelDestination.LOGS_ROUTE
            else -> null
        }

    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar

    val smartTopLevelDestinations: List<SmartTopLevelDestination> = SmartTopLevelDestination.entries
}
