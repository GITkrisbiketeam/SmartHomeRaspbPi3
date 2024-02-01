package com.krisbiketeam.smarthomeraspbpi3.ui.compose.core.drawer

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room

data class SmartDrawerUiState(
    val user: String = "Login to Firebase",
    val home: String = "Setup Home",
    val alarmEnabled: Boolean? = null,
    val onlineStatus: String? = null,
    val roomList: List<Room>? = null
)