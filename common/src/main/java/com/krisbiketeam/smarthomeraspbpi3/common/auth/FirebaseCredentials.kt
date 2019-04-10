package com.krisbiketeam.smarthomeraspbpi3.common.auth

data class FirebaseCredentials(val email: String, val password: String) {
    override fun toString(): String {
        return "FirebaseCredentials(email=$email, password=${password.replace(Regex("."), "*")})"
    }
}
