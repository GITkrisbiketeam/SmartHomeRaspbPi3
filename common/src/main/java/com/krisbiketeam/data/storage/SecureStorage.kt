package com.krisbiketeam.data.storage

import android.content.Context
import com.krisbiketeam.data.auth.FirebaseCredentials

interface SecureStorage {
    fun saveFirebaseCredentials(firebaseCredentials: FirebaseCredentials)
    fun retrieveFirebaseCredentials(): FirebaseCredentials?
    fun isAuthenticated(): Boolean
}

// Todo: implement a encrypted secure storage since this is not secure
class NotSecureStorage(context: Context) : SecureStorage {
    companion object {
        private const val SHARED_FILE = "androidThingsExample"
        private const val EMAIL_KEY = "secureEmailKey"
        private const val PASSWORD_KEY = "securePasswordKey"
    }

    private val sharedPreferences = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()
    override fun saveFirebaseCredentials(firebaseCredentials: FirebaseCredentials) {
        editor.putString(EMAIL_KEY, firebaseCredentials.email)
        editor.putString(PASSWORD_KEY, firebaseCredentials.password)
        editor.apply()
    }

    override fun retrieveFirebaseCredentials(): FirebaseCredentials? {
        val email = sharedPreferences.getString(EMAIL_KEY, "")
        val password = sharedPreferences.getString(PASSWORD_KEY, "")
        return when {
            email.isNotEmpty() && password.isNotEmpty() ->
                FirebaseCredentials(email, password)
            else -> null
        }
    }

    override fun isAuthenticated(): Boolean {
        return retrieveFirebaseCredentials() != null
    }
}
