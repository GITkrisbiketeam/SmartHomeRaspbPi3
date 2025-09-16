package com.krisbiketeam.smarthomeraspbpi3.compose.components.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R

@Composable
fun SettingsClickableComp(
    @StringRes name: Int,
    @DrawableRes icon: Int? = null,
    @StringRes subText: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.surfaceTint,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .padding(16.dp),
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null
                )
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
            SettingsClickableComp(R.string.settings_login_title, R.drawable.person_24px) {}
            SettingsClickableComp(R.string.settings_home_title, R.drawable.house_24) {}
            SettingsClickableComp(R.string.hw_unit_list_title, R.drawable.list_24px) {}
            SettingsClickableComp(
                R.string.hw_unit_error_event_list_title,
                R.drawable.playlist_remove_24px
            ) {}
            SettingsClickableComp(R.string.things_app_logs_title, R.drawable.view_list_24px) {}
            SettingsClickableComp(R.string.hw_unit_error_logs_title, R.drawable.data_alert_24px) {}

            SettingsClickableComp(R.string.settings_restart_pi, R.drawable.restart_alt_24px) {}
        }
    }
}
