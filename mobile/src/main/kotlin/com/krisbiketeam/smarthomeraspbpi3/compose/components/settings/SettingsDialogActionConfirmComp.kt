package com.krisbiketeam.smarthomeraspbpi3.compose.components.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R

@Composable
fun SettingsTextComp(
    @StringRes name: Int,
    @StringRes dialogDescription: Int,
    @StringRes state: Int? = null,
    @DrawableRes icon: Int? = null,
    onConfirmed: () -> Unit,
) {

    // if the dialog is visible
    var isDialogShown by remember {
        mutableStateOf(false)
    }

    // conditional visibility in dependence to state
    if (isDialogShown) {
        AlertDialog(
            icon = {
                Icon(
                    painterResource(id = R.drawable.warning_24px),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Red
                )
            },
            title = {
                Text(text = stringResource(id = name))
            },
            text = {
                Text(text = stringResource(id = dialogDescription))
            },
            onDismissRequest = {
                // dismiss the dialog on touch outside
                isDialogShown = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmed()
                        isDialogShown = false
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isDialogShown = false
                    }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            })
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = {
            // clicking on the preference, will show the dialog
            isDialogShown = true
        },
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (icon != null) {
                    Icon(
                        painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    // setting text title
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                    if (state != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // current value shown
                        Text(
                            text = stringResource(id = state),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun LogsTopAppBarPreview() {
    MaterialTheme {
        Column {
            SettingsTextComp(
                name = R.string.settings_restart_pi,
                dialogDescription = R.string.settings_restart_alert_description,
                state = R.string.settings_restarting,
                icon = R.drawable.restart_alt_24px
            ) {}

            SettingsTextComp(
                name = R.string.settings_restart_pi,
                dialogDescription = R.string.settings_restart_alert_description,
                icon = R.drawable.restart_alt_24px
            ) {}
        }
    }
}
