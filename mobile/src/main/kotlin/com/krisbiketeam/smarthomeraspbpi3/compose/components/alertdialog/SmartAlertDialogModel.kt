package com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog

import androidx.annotation.StringRes

data class SmartAlertDialogModel(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val positiveButtonTextId: Int? = null,
    val positiveButtonAction: ()-> Unit
)