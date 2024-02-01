package com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.krisbiketeam.smarthomeraspbpi3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartUnitCard(
    model: SmartUnitCardModel,
    onClick: () -> Unit,
    onSwitch: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        colors = if (model.error) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(id = R.dimen.cardview_default_elevation)
        ),
        modifier = modifier
            .padding(horizontal = dimensionResource(id = R.dimen.margin_small))
            .padding(bottom = dimensionResource(id = R.dimen.margin_normal))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = model.title,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(id = R.dimen.margin_normal))
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
            if (model.subtitle != null) {
                Text(
                    text = model.subtitle,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(id = R.dimen.margin_normal))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
            if (model.switchState != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            start = dimensionResource(id = R.dimen.margin_normal),
                            end = dimensionResource(id = R.dimen.margin_normal)
                        ),
                    horizontalArrangement = if (model.switchText != null) Arrangement.Absolute.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (model.switchText != null) {
                        Text(
                            text = model.switchText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                end = dimensionResource(id = R.dimen.margin_normal)
                            )
                        )
                    }
                    Switch(
                        checked = model.switchState,
                        onCheckedChange = { checked ->
                            onSwitch(checked)
                        },
                    )
                }

            }
        }
    }
}

@Preview
@Composable
private fun SmartUnitCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                SmartUnitCard(SmartUnitCardModel("Title 1"), {}, {})
                SmartUnitCard(SmartUnitCardModel("Title 2", "Subtitle 2"), {}, {})
                SmartUnitCard(SmartUnitCardModel("Title 3", "Subtitle 3", error = true), {}, {})
                SmartUnitCard(SmartUnitCardModel("Title 4", "Subtitle 4", false), {}, {})
                SmartUnitCard(SmartUnitCardModel("Title 5", "Subtitle 5", true), {}, {})
                SmartUnitCard(SmartUnitCardModel( "Title 6", "Subtitle 6", true, "Switch"), {}, {})
            }
        }
    }
}