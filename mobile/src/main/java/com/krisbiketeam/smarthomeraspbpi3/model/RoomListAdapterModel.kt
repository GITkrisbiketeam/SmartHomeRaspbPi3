package com.krisbiketeam.smarthomeraspbpi3.model

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room

data class RoomListAdapterModel(val room: Room?, var homeUnit: HomeUnit<Any?>? = null, var error: Boolean = false)