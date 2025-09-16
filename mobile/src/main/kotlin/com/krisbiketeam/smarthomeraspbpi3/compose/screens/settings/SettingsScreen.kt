package com.krisbiketeam.smarthomeraspbpi3.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.compose.components.settings.SettingsClickableComp
import com.krisbiketeam.smarthomeraspbpi3.compose.components.settings.SettingsGroup
import com.krisbiketeam.smarthomeraspbpi3.compose.components.settings.SettingsSwitchComp
import com.krisbiketeam.smarthomeraspbpi3.compose.components.settings.SettingsTextComp
import com.krisbiketeam.smarthomeraspbpi3.compose.components.topappbat.SettingsTopAppBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    navigateToLogin: () -> Unit,
    navigateToHomeSetup: () -> Unit,
    navigateToHwUnitList: () -> Unit,
    navigateToHwUnitErrorList: () -> Unit,
    navigateToLogsList: () -> Unit,
    navigateToHwUnitErrorLogsList: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SettingsTopAppBar { navigateUp() }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SettingsSwitchComp(
                name = R.string.settings_alart_text,
                icon = R.drawable.safety_check_24px,
                state = uiState.alarmEnabled
            ) {
                viewModel.setAlarmState(!uiState.alarmEnabled)
            }
            SettingsGroup(name = R.string.settings_group_account) {
                SettingsClickableComp(
                    R.string.settings_login_title,
                    R.drawable.person_24px
                ) { navigateToLogin() }
                SettingsClickableComp(
                    R.string.settings_home_title,
                    R.drawable.house_24
                ) { navigateToHomeSetup() }
            }
            SettingsGroup(name = R.string.settings_group_units_and_logs) {
                SettingsClickableComp(
                    R.string.hw_unit_list_title,
                    R.drawable.list_24px
                ) { navigateToHwUnitList() }
                SettingsClickableComp(
                    R.string.hw_unit_error_event_list_title,
                    R.drawable.playlist_remove_24px
                ) { navigateToHwUnitErrorList() }
                SettingsClickableComp(
                    R.string.things_app_logs_title,
                    R.drawable.view_list_24px
                ) { navigateToLogsList() }
                SettingsClickableComp(
                    R.string.hw_unit_error_logs_title,
                    R.drawable.data_alert_24px
                ) { navigateToHwUnitErrorLogsList() }
            }
            SettingsGroup(name = R.string.settings_group_other) {
                SettingsTextComp(
                    name = R.string.settings_restart_pi,
                    dialogDescription = R.string.settings_restart_alert_description,
                    state = if (uiState.appRestarting) R.string.settings_restarting else null
                ) {
                    viewModel.resetRPi3App()
                }
            }
        }
    }
}

/*
@Preview
@Composable
private fun LogsTopAppBarPreview() {
    MaterialTheme {
        Surface {
            SettingsScreen({})
        }
    }
}
*/
