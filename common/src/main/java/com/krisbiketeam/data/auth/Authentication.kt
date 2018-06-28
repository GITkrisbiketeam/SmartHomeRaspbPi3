package com.krisbiketeam.data.auth

import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

interface Authentication {
    fun login(firebaseCredentials: FirebaseCredentials)
    fun addLoginResultListener(loginResultListener: LoginResultListener)

    interface LoginResultListener {
        fun success()
        fun failed(exception: Exception)
    }
}

class FirebaseAuthentication : Authentication {
    private val firebaseAuth = FirebaseAuth.getInstance()
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
        firebaseAuth.signInWithEmailAndPassword(firebaseCredentials.email, firebaseCredentials.password)
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Timber.e(exception, "Login User failed!")
                    loginResultListener?.failed(exception)
                }
                .addOnSuccessListener {
                    Timber.d("Login user success!")
                    loginResultListener?.success()
                }
    }
}
