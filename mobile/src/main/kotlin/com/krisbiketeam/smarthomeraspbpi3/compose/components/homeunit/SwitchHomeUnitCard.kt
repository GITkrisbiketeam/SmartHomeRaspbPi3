package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

@Composable
fun SwitchHomeUnitCard(
    model: HomeUnitCardModel.SwitchHomeUnitCardModel,
    onSwitch: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Switch(
            modifier = Modifier.padding(end = 8.dp),
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
                SwitchHomeUnitCard(
                    HomeUnitCardModel.SwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch"),
                        "Switch",
                        true,
                        123456789
                    ),
                ) {}
                SwitchHomeUnitCard(
                    HomeUnitCardModel.SwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch"),
                        "Switch",
                        null,
                        123456789
                    ),
                ) {}
            }
        }
    }
}