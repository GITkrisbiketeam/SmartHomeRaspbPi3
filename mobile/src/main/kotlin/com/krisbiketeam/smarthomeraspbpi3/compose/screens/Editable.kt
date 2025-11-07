package com.krisbiketeam.smarthomeraspbpi3.compose.screens

data class Editable<E>(val value: E, val editAction: () -> Unit)
