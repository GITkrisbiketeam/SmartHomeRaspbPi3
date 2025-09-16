package com.krisbiketeam.smarthomeraspbpi3.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Green80 = Color(0xFFE6F3DC)
val Yellow80 = Color(0xFFF3EDB6)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val Green40 = Color(0xFF004B00)
val Yellow40 = Color(0xFF4B4B00)

val Green
    @Composable
    get() = if (isSystemInDarkTheme()) Green40 else Green80
val Yellow
    @Composable
    get() = if (isSystemInDarkTheme()) Yellow40 else Yellow80