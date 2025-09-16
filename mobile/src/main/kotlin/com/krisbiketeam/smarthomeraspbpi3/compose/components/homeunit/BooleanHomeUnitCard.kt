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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

@Composable
fun BooleanHomeUnitCard(
    model: HomeUnitCardModel.BooleanHomeUnitCardModel,
) {
    Text(
        text = model.value.toString(),
        textAlign = TextAlign.Center,
        maxLines = 1,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

@Preview
@Composable
private fun HomeUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                BooleanHomeUnitCard(
                    HomeUnitCardModel.BooleanHomeUnitCardModel(
                        HomeUnitCardModelId(HomeUnitType.HOME_MOTIONS, "Motion"),
                        "Motion",
                        false,
                        123456789
                    )
                )
            }
        }
    }
}