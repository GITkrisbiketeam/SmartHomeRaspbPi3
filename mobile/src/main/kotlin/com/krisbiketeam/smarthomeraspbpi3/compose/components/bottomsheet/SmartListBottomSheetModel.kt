package com.krisbiketeam.smarthomeraspbpi3.compose.components.alertdialog

import androidx.annotation.StringRes
import com.krisbiketeam.smarthomeraspbpi3.R

sealed interface SmartListBottomSheetModel<T, R> {

    val title: Int
    val list: List<R>
    val positiveButtonTextId: Int
    val positiveButtonAction: (T) -> Unit
    val preselection: String?

    data class NonEmpty(
        @StringRes override val title: Int,
        override val list: List<String>,
        @StringRes override val positiveButtonTextId: Int = R.string.menu_save,
        override val positiveButtonAction: (String) -> Unit,
        override val preselection: String? = null,
    ) : SmartListBottomSheetModel<String, String>

    data class NonEmptyUsed(
        @StringRes override val title: Int,
        override val list: List<Pair<String, Boolean>>,
        @StringRes override val positiveButtonTextId: Int = R.string.menu_save,
        override val positiveButtonAction: (String) -> Unit,
        override val preselection: String? = null,
    ) : SmartListBottomSheetModel<String, Pair<String, Boolean>>

    data class WithEmpty(
        @StringRes override val title: Int,
        override val list: List<String>,
        @StringRes override val positiveButtonTextId: Int = R.string.menu_save,
        override val positiveButtonAction: (String?) -> Unit,
        override val preselection: String? = null,
    ) : SmartListBottomSheetModel<String?, String>

    data class WithEmptyUsed(
        @StringRes override val title: Int,
        override val list: List<Pair<String, Boolean>>,
        @StringRes override val positiveButtonTextId: Int = R.string.menu_save,
        override val positiveButtonAction: (String?) -> Unit,
        override val preselection: String? = null,
    ) : SmartListBottomSheetModel<String?, Pair<String, Boolean>>
}

