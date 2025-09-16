package com.krisbiketeam.smarthomeraspbpi3.compose.components.bottomnavigationbar

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartTopLevelDestination
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.isTopLevelDestinationInHierarchy

@Composable
fun SmartBottomBar(
    destinations: List<SmartTopLevelDestination>,
    smartNavigationActions: SmartNavigationActions,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {

        destinations.forEach { destination ->
            NavigationBarItem(
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