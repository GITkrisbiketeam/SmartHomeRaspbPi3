package com.krisbiketeam.smarthomeraspbpi3.compose.components.sidenavigationrail

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartTopLevelRoute
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.isTopLevelDestinationInHierarchy

@Composable
fun SmartSideNavigationRail(
    smartTopLevelRoutes: List<SmartTopLevelRoute<*>>,
    smartNavigationActions: SmartNavigationActions,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {

    NavigationRail(
        modifier = modifier
    ) {
        smartTopLevelRoutes.forEach { destination ->
            NavigationRailItem(
                selected = currentDestination.isTopLevelDestinationInHierarchy(destination),
                onClick = { smartNavigationActions.navigateToSmartTopLevelDestination(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = null,
                        //tint = tintColor
                    )
                },
                modifier = modifier,
                label = { Text(stringResource(destination.titleTextId)) },
            )
        }
    }
}