package com.krisbiketeam.data.auth

data class FirebaseCredentials(val email: String, val password: String) {
    override fun toString(): String {
        return "FirebaseCredentials(email=$email, password=${password.replace(Regex("."), "*")})"
    }
}
