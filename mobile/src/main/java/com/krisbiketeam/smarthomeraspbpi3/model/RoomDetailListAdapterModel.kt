package com.krisbiketeam.smarthomeraspbpi3.model

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit

data class RoomDetailListAdapterModel(var homeUnit: HomeUnit<Any>, var error: Boolean)