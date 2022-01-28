package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

data class RemoteLog(
        var priority: String,
        var tag: String?,
        var message: String,
        var throwable: String?,
        val time : String
)