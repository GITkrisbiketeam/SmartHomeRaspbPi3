package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class RemoteLog(
        var priority: String = "",
        var tag: String? = null,
        var message: String = "",
        var throwable: String? = null,
        val time: String = ""
)