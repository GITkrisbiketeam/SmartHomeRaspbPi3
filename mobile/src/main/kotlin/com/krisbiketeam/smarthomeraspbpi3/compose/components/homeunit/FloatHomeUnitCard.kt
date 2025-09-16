package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.utils.getStringValue

@Composable
fun FloatHomeUnitCard(
    model: HomeUnitCardModel.FloatHomeUnitCardModel,
) {
    Text(
        text = model.value.getStringValue(),
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
}

@Preview
@Composable
private fun HomeUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                FloatHomeUnitCard(
                    HomeUnitCardModel.FloatHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_TEMPERATURES, "temp"),
                        "Temperature",
                        20.1,
                        13456789
                    ))
            }
        }
    }
}