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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.utils.getStringValue

@Composable
fun WaterCirculationHomeUnitCard(
    model: HomeUnitCardModel.WaterCirculationHomeUnitCardModel,
    onSwitch: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = model.temperatureValue.getStringValue(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.margin_small),
                    end = dimensionResource(id = R.dimen.margin_small),
                    bottom = dimensionResource(id = R.dimen.margin_small)
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
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
                        text = when (model.motionValue) {
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
}

@Preview
@Composable
private fun HomeUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                WaterCirculationHomeUnitCard(
                    HomeUnitCardModel.WaterCirculationHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Water circulation", "hwUnit"),
                        "Water circulation",
                        true,
                        123456789,
                        false,
                        1234567890,
                        27,
                        1234567890,
                    )
                ) {}
                WaterCirculationHomeUnitCard(
                    HomeUnitCardModel.WaterCirculationHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Water circulation", "hwUnit"),
                        "Water circulation",
                        null,
                        123456789,
                        null,
                        1234567890,
                        null,
                        1234567890,
                    )
                ) {}
            }
        }
    }
}