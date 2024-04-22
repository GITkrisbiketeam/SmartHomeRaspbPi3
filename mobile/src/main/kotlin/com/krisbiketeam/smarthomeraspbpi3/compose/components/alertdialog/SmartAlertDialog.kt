package com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.krisbiketeam.smarthomeraspbpi3.R

@Composable
fun SmartAlertDialog(
    model: SmartAlertDialogModel,
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = model.title))
        },
        text = {
            Text(text = stringResource(id = model.description))
        },
        onDismissRequest = {
            onCancelClick()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOkClick()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onCancelClick()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun SmartAlertDialogPreview() {
    MaterialTheme {
        Surface {
            SmartAlertDialog(
                SmartAlertDialogModel(
                    R.string.save_room,
                    R.string.add_edit_home_unit_overwrite_changes,
                    R.string.overwrite, {}
                ), {}, {})
        }
    }
}