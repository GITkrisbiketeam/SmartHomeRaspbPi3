package com.krisbiketeam.smarthomeraspbpi3.compose.components.homeunit

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

sealed interface HomeUnitCardModel {
    val id: HomeUnitCardModelId
    val title: String

    //val value: String
    val updateTime: Long?
    val isError: Boolean

    data class FloatHomeUnitCardModel(
        override val id: HomeUnitCardModelId,
        override val title: String,
        val value: Number?,
        override val updateTime: Long?,
        override val isError: Boolean = false
    ) : HomeUnitCardModel

    data class BooleanHomeUnitCardModel(
        override val id: HomeUnitCardModelId,
        override val title: String,
        val value: Boolean?,
        override val updateTime: Long?,
        override val isError: Boolean = false
    ) : HomeUnitCardModel

    data class SwitchHomeUnitCardModel(
        override val id: HomeUnitCardModelId,
        override val title: String,
        val value: Boolean?,
        override val updateTime: Long?,
        override val isError: Boolean = false
    ) : HomeUnitCardModel

    data class LightSwitchHomeUnitCardModel(
        override val id: HomeUnitCardModelId,
        override val title: String,
        val value: Boolean?,
        override val updateTime: Long?,
        val switchValue: Boolean?,
        val switchUpdateTime: Long?,
        override val isError: Boolean = false
    ) : HomeUnitCardModel

    data class WaterCirculationHomeUnitCardModel(
        override val id: HomeUnitCardModelId,
        override val title: String,
        val value: Boolean?,
        override val updateTime: Long?,
        val motionValue: Boolean?,
        val motionUpdateTime: Long?,
        val temperatureValue: Number?,
        val temperatureUpdateTime: Long?,
        override val isError: Boolean = false
    ) : HomeUnitCardModel
}

data class HomeUnitCardModelId(
    val homeUnitType: HomeUnitType,
    val homeUnitName: String,
    val hwUnitName: String? = null
)