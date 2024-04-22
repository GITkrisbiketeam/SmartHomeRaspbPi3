package com.krisbiketeam.smarthomeraspbpi3.compose.components.smartcard

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

data class SmartUnitCardModel(
    val title: String,
    val subtitle: String? = null,
    val switchState: Boolean? = null,
    val switchText: String? = null,
    val switchUnit: Pair<HomeUnitType, String>? = null,
    val background: CardColorState = CardColorState.NONE
)

enum class CardColorState{
    NONE,
    ERROR,
    MOTION,
    REED_SWITCH
}