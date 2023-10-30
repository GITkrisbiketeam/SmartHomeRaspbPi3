package com.krisbiketeam.smarthomeraspbpi3.common.auth

import kotlinx.serialization.Serializable

@Serializable
data class WifiCredentials(val ssid: String, val password: String){
    override fun toString(): String {
        return "WifiCredentials(ssid=$ssid, password=${password.replace(Regex("."), "*")})"
    }
}
