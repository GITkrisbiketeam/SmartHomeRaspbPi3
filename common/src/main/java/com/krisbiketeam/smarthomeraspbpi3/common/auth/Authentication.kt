package com.krisbiketeam.smarthomeraspbpi3.common.auth

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import timber.log.Timber

interface Authentication {
    fun login(firebaseCredentials: FirebaseCredentials)
    fun addLoginResultListener(loginResultListener: LoginResultListener)

    interface LoginResultListener {
        fun success(uid: String?)
        fun failed(exception: Exception)
    }
}

class FirebaseAuthentication : Authentication {
    private var loginResultListener: Authentication.LoginResultListener? = null

    override fun login(firebaseCredentials: FirebaseCredentials) {
        Timber.d("Login in user")
        loginUser(firebaseCredentials)
    }

    override fun addLoginResultListener(loginResultListener: Authentication.LoginResultListener) {
        this.loginResultListener = loginResultListener
    }

    // First we try to create the user
    private fun loginUser(firebaseCredentials: FirebaseCredentials) {
        Firebase.auth.signInWithEmailAndPassword(firebaseCredentials.email, firebaseCredentials.password)
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Timber.e(exception, "Login User failed!")
                    loginResultListener?.failed(exception)
                }
                .addOnSuccessListener {
                    Timber.d("Login user success! user uid: ${it.user?.uid}")
                    loginResultListener?.success(it.user?.uid)
                }
    }
}
