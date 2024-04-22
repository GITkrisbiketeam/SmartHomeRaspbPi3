package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

@Composable
fun LightSwitchHomeUnitCard(
    model: HomeUnitCardModel.LightSwitchHomeUnitCardModel,
    onSwitch: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SuggestionChip(
            onClick = {},
            label = {
                Text(
                    text = when (model.switchValue) {
                        true -> "On"
                        false -> "Off"
                        null -> "null"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            })
        Switch(
            modifier = Modifier
                .wrapContentWidth(),
            checked = model.value ?: false,
            thumbContent = if (model.value == null) {
                { Icon(Icons.Outlined.Close, contentDescription = null) }
            } else null,
            onCheckedChange = { checked ->
                onSwitch(checked)
            },
        )
    }
}

@Preview
@Composable
private fun HomeUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                LightSwitchHomeUnitCard(
                    HomeUnitCardModel.LightSwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch"),
                        "Switch",
                        true,
                        123456789,
                        false,
                        1234567890
                    )
                ) {}
                LightSwitchHomeUnitCard(
                    HomeUnitCardModel.LightSwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch"),
                        "Switch",
                        null,
                        123456789,
                        null,
                        null
                    ),
                ) {}
            }
        }
    }
}