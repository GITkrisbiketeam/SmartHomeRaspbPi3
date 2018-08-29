package com.krisbiketeam.data.auth

data class WifiCredentials(val ssid: String, val password: String){
    override fun toString(): String {
        return "WifiCredentials(ssid=$ssid, password=${password.replace(Regex("."), "*")})"
    }
}
