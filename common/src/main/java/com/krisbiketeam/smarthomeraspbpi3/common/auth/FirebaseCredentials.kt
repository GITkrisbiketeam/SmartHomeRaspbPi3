package com.krisbiketeam.smarthomeraspbpi3.common.auth

data class FirebaseCredentials(val email: String, val password: String, val uid: String? = null) {
    override fun toString(): String {
        return "FirebaseCredentials(email=$email, password=${password.replace(Regex("."), "*")}, uid=$uid)"
    }
}
