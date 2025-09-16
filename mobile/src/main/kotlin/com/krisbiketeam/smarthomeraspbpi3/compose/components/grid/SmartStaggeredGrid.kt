package com.krisbiketeam.smarthomeraspbpi3.compose.components.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard.SmartUnitCard
import com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard.SmartUnitCardModel

@Composable
fun SmartStaggeredGrid(
    models: List<SmartUnitCardModel>,
    onClicked: (SmartUnitCardModel) -> Unit,
    onSwitched: (SmartUnitCardModel, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(100.dp),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = dimensionResource(id = R.dimen.margin_small)),
        content = {
            items(models) { model ->
                SmartUnitCard(
                    model = model,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    onClick = { onClicked(model) },
                    onSwitch = { checked ->
                        onSwitched(model, checked)
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Preview
@Composable
private fun SmartUnitCardPreview() {
    MaterialTheme {
        Surface {
            SmartStaggeredGrid(
                listOf(
                    SmartUnitCardModel("Title 1"),
                    SmartUnitCardModel("Title 2", "Subtitle 2"),
                    SmartUnitCardModel( "Title 3", "Subtitle 3"),
                    SmartUnitCardModel("Title 4"),
                    SmartUnitCardModel("Title 5", "Subtitle 5", false),
                    SmartUnitCardModel("Title 6", "Subtitle 6", true),
                    SmartUnitCardModel("Very Long Title 7", "Subtitle 7", true),
                    SmartUnitCardModel("Title 8", "Subtitle 8", true, "Light")
                ), { }, { _, _ -> }
            )
        }
    }
}