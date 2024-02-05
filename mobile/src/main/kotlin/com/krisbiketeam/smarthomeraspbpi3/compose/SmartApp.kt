package com.krisbiketeam.smarthomeraspbpi3.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krisbiketeam.smarthomeraspbpi3.compose.components.bottomnavigationbar.SmartBottomBar
import com.krisbiketeam.smarthomeraspbpi3.compose.components.sidenavigationrail.SmartSideNavigationRail
import com.krisbiketeam.smarthomeraspbpi3.compose.core.drawer.SmartModalDrawer
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinationsArgs
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartGraphs
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavGraph
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartTopLevelDestination
import kotlinx.coroutines.CoroutineScope

@Composable
fun SmartApp(
    windowSizeClass: WindowSizeClass,
    startDestination: String = SmartGraphs.ROOM_LIST_GRAPH_ROOT
) {

    val navController: NavHostController = rememberNavController()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val smartNavActions: SmartNavigationActions = remember(navController) {
        SmartNavigationActions(navController)
    }
    val drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination

    SmartModalDrawer(
        drawerState,
        currentRoute?.route ?: startDestination,
        smartNavActions,
        currentNavBackStackEntry?.arguments?.getString(SmartDestinationsArgs.ROOM_NAME_ARG)
            ?: "null"
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    SmartBottomBar(
                        destinations = SmartTopLevelDestination.entries,
                        smartNavigationActions = smartNavActions,
                        currentDestination = currentRoute,
                    )
                }
            },
        ) { padding ->

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {
                    SmartSideNavigationRail(
                        destinations = SmartTopLevelDestination.entries,
                        smartNavigationActions = smartNavActions,
                        currentDestination = currentRoute,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }

                Column(Modifier.fillMaxSize()) {
                    SmartNavGraph(
                        navController,
                        coroutineScope,
                        smartNavActions,
                        drawerState,
                        startDestination
                    )
                }
            }
        }
    }
}