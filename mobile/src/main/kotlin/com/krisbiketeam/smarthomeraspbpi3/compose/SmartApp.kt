package com.krisbiketeam.smarthomeraspbpi3.compose

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
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krisbiketeam.smarthomeraspbpi3.compose.components.bottomnavigationbar.SmartBottomBar
import com.krisbiketeam.smarthomeraspbpi3.compose.components.sidenavigationrail.SmartSideNavigationRail
import com.krisbiketeam.smarthomeraspbpi3.compose.core.drawer.SmartModalDrawer
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.RoomListGraph
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavGraph
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.smartTopLevelRoutes
import kotlinx.coroutines.CoroutineScope

@Composable
fun SmartApp(
    windowSizeClass: WindowSizeClass,
    startDestination: Any = RoomListGraph
) {

    val navController: NavHostController = rememberNavController()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val smartNavActions: SmartNavigationActions = remember(navController) {
        SmartNavigationActions(navController)
    }
    val drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerGesturesEnabled = rememberSaveable { mutableStateOf(true) }


    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination

    SmartModalDrawer(
        drawerState,
        drawerGesturesEnabled.value,
        smartNavActions,
        currentNavBackStackEntry
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    SmartBottomBar(
                        smartTopLevelRoutes = smartTopLevelRoutes,
                        smartNavigationActions = smartNavActions,
                        currentDestination = currentRoute,
                    )
                }
            },
        ) { padding ->
            if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {
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
                    SmartSideNavigationRail(
                        smartTopLevelRoutes = smartTopLevelRoutes,
                        smartNavigationActions = smartNavActions,
                        currentDestination = currentRoute,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    SmartNavGraph(
                        navController,
                        coroutineScope,
                        smartNavActions,
                        drawerState,
                        { enabled -> drawerGesturesEnabled.value = enabled },
                        startDestination
                    )
                }
            } else {
                Surface(modifier = Modifier.padding(padding)) {
                    SmartNavGraph(
                        navController,
                        coroutineScope,
                        smartNavActions,
                        drawerState,
                        { enabled ->
                            drawerGesturesEnabled.value = enabled
                        },
                        startDestination
                    )
                }
            }
        }
    }
}