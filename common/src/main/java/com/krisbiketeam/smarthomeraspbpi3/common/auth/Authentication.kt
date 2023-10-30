package com.krisbiketeam.smarthomeraspbpi3.common.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

interface Authentication {
    fun login(firebaseCredentials: FirebaseCredentials)
    suspend fun loginSuspend(firebaseCredentials: FirebaseCredentials): Boolean
    fun addLoginResultListener(loginResultListener: LoginResultListener)
    fun removeLoginResultListener()

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

    override suspend fun loginSuspend(firebaseCredentials: FirebaseCredentials): Boolean =
        suspendCancellableCoroutine { continuation ->
            Timber.d("Login in user")
            loginResultListener = object : Authentication.LoginResultListener {
                override fun success(uid: String?) {
                    Timber.d("LoginResultListener success $uid")
                    continuation.resume(true)
                }

                override fun failed(exception: Exception) {
                    Timber.d("LoginResultListener failed e: $exception")
                    continuation.resume(false)
                }
            }
            loginUser(firebaseCredentials)

            continuation.invokeOnCancellation {
                loginResultListener = null
            }
        }

    override fun addLoginResultListener(loginResultListener: Authentication.LoginResultListener) {
        this.loginResultListener = loginResultListener
    }

    override fun removeLoginResultListener() {
        this.loginResultListener = null
    }

    // First we try to create the user
    private fun loginUser(firebaseCredentials: FirebaseCredentials): Task<AuthResult> {
        return Firebase.auth.signInWithEmailAndPassword(
            firebaseCredentials.email,
            firebaseCredentials.password
        )
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Timber.e("Login User failed! $exception")
                loginResultListener?.failed(exception)
            }
            .addOnSuccessListener {
                Timber.d("Login user success! user uid: ${it.user?.uid}")
                loginResultListener?.success(it.user?.uid)
            }
    }
}
