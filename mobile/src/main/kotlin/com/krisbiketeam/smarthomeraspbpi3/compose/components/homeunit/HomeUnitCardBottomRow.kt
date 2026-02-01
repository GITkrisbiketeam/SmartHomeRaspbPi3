package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.utils.getUpdatedAgo

@Composable
fun HomeUnitCardBottomRow(
    updateTime: Long?,
    showLogs: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.margin_small))
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween

    ) {
        FilterChip(
            selected = true,
            onClick = { },
            modifier = Modifier
                .weight(weight = 1F, fill = false)
                .wrapContentWidth()
                .padding(end = 8.dp),
            label = {
                Text(
                    text = updateTime.getUpdatedAgo(LocalContext.current),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
        if (showLogs != null) {
            Icon(
                painter = painterResource(R.drawable.ic_statistics),
                contentDescription = null,
                modifier = Modifier
                    .size(32.0.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.shapes.small
                    )
                    .padding(4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        role = Role.Button
                    ) { showLogs() },
                tint = Color.Unspecified
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
                HomeUnitCardBottomRow(
                    123456789
                ) {}
            }
        }
    }
}