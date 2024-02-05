package com.krisbiketeam.smarthomeraspbpi3.compose.core.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartDestinations
import com.krisbiketeam.smarthomeraspbpi3.compose.navigation.SmartNavigationActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SmartModalDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    navigationActions: SmartNavigationActions,
    currentRouteArgs: String? = null,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    viewModel: SmartDrawerViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ModalDrawerSheet {
                AppDrawer(
                    uiState,
                    currentRoute = currentRoute,
                    currentRouteArgs = currentRouteArgs,
                    navigateToRoomList = { navigationActions.navigateToRoomList() },
                    navigateToTaskList = { navigationActions.navigateToTaskList() },
                    navigateToRoomDetail = { navigationActions.navigateToRoomDetail(it) },
                    closeDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        content()
    }
}

@Composable
private fun AppDrawer(
    uiState: SmartDrawerUiState,
    currentRoute: String,
    currentRouteArgs: String?,
    navigateToRoomList: () -> Unit,
    navigateToTaskList: () -> Unit,
    navigateToRoomDetail: (String) -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DrawerHeader(uiState)

        uiState.roomList?.let { rooms ->
            DrawerRoomList(rooms, currentRoute, currentRouteArgs, navigateToRoomDetail, closeDrawer)

            Divider(
                modifier = Modifier.padding(horizontal = 28.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }


        DrawerButton(
            painter = painterResource(id = R.drawable.ic_baseline_other_houses_24),
            label = stringResource(id = R.string.room_list_title),
            isSelected = currentRoute == SmartDestinations.ROOM_LIST_ROUTE,
            action = {
                navigateToRoomList()
                closeDrawer()
            }
        )

        DrawerButton(
            painter = painterResource(id = R.drawable.ic_baseline_view_headline_24),
            label = stringResource(id = R.string.task_list_title),
            isSelected = currentRoute == SmartDestinations.TAK_LIST_ROUTE,
            action = {
                navigateToTaskList()
                closeDrawer()
            }
        )
    }
}

@Composable
private fun DrawerHeader(
    uiState: SmartDrawerUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .wrapContentHeight()
            .padding(dimensionResource(id = R.dimen.margin_normal))
    ) {
        Row(modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.margin_small))) {
            Icon(Icons.Filled.Home, null)
            Text(
                text = uiState.home,
                color = MaterialTheme.colorScheme.surface,

                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.margin_small))) {
            Icon(Icons.Filled.AccountBox, null)
            Text(
                text = uiState.user,
                color = MaterialTheme.colorScheme.surface,

                modifier = Modifier.padding(start = 8.dp)
            )
        }
        uiState.onlineStatus?.let { status ->
            Text(
                text = status,
                color = MaterialTheme.colorScheme.surface,

                modifier = Modifier.padding(start = 8.dp)
            )
        }
        uiState.alarmEnabled?.let { enabled ->
            Text(
                text = if (enabled) "Alarm Enabled" else "Alarm Disabled",
                color = MaterialTheme.colorScheme.surface,

                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun DrawerRoomList(
    rooms: List<Room>,
    currentRoute: String,
    currentRouteArgs: String?,
    navigateToRoomDetail: (String) -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(dimensionResource(id = R.dimen.margin_normal))
    ) {
        rooms.forEach { room ->
            DrawerButton(
                painter = painterResource(id = R.drawable.ic_outline_label_24),
                label = room.name,
                isSelected = currentRoute == SmartDestinations.ROOM_DETAIL_ROUTE && currentRouteArgs == room.name,
                action = {
                    navigateToRoomDetail(room.name)
                    closeDrawer()
                }
            )

        }
    }

}

@Composable
private fun DrawerButton(
    painter: Painter,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    TextButton(
        onClick = action,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.activity_horizontal_margin))
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painter,
                contentDescription = null, // decorative
                tint = tintColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = tintColor
            )
        }
    }
}

@Preview("Drawer contents")
@Composable
fun PreviewAppDrawer() {
    MaterialTheme {
        Surface {
            AppDrawer(
                SmartDrawerUiState("someuyser@gmail.com", "Home Name"),
                currentRoute = SmartDestinations.ROOM_LIST_ROUTE,
                currentRouteArgs = null,
                navigateToRoomList = {},
                navigateToTaskList = {},
                navigateToRoomDetail = {},
                closeDrawer = {}
            )
        }
    }
}
