package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

@Composable
fun HomeUnitCard(
    homeUnitModel: HomeUnitCardModel,
    onClick: () -> Unit,
    onSwitch: (Boolean) -> Unit,
    showLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        colors = if (homeUnitModel.isError) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        modifier = modifier
    ) {
        Column(
            Modifier.wrapContentWidth(),
        ) {
            Text(
                text = homeUnitModel.title,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
            when (homeUnitModel) {
                is HomeUnitCardModel.FloatHomeUnitCardModel -> {
                    FloatHomeUnitCard(model = homeUnitModel)
                }

                is HomeUnitCardModel.BooleanHomeUnitCardModel -> {
                    BooleanHomeUnitCard(model = homeUnitModel)
                }

                is HomeUnitCardModel.SwitchHomeUnitCardModel -> {
                    SwitchHomeUnitCard(
                        model = homeUnitModel,
                        onSwitch = onSwitch
                    )
                }

                is HomeUnitCardModel.LightSwitchHomeUnitCardModel -> {

                    LightSwitchHomeUnitCard(
                        model = homeUnitModel,
                        onSwitch = onSwitch
                    )
                }
            }

            HomeUnitCardBottomRow(
                homeUnitModel.updateTime,
                homeUnitModel.id.hwUnitName?.let { showLogs })
        }
    }
}

@Preview
@Composable
private fun HomeUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.widthIn(max = 200.dp)) {
                HomeUnitCard(
                    HomeUnitCardModel.FloatHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_TEMPERATURES, "temp", "hwTemp"),
                        "Temperature",
                        20.1,
                        13456789
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )

                HomeUnitCard(
                    HomeUnitCardModel.BooleanHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_MOTIONS, "Motion", "hwMotion"),
                        "Motion",
                        false,
                        123456789
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )

                HomeUnitCard(
                    HomeUnitCardModel.SwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch", "hwUnit"),
                        "Switch",
                        true,
                        123456789
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )
                HomeUnitCard(
                    HomeUnitCardModel.SwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch", "hwUnit"),
                        "Switch",
                        null,
                        123456789
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )

                HomeUnitCard(
                    HomeUnitCardModel.LightSwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch", "hwUnit"),
                        "Switch",
                        true,
                        123456789,
                        false,
                        1234567890
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )
                HomeUnitCard(
                    HomeUnitCardModel.LightSwitchHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_ACTUATORS, "Switch", "hwUnit"),
                        "Switch",
                        null,
                        123456789,
                        null,
                        null
                    ), {}, {}, {},
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}