package com.krisbiketeam.smarthomeraspbpi3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

class LoginActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder().build()
    private lateinit var authentication: Authentication
    private lateinit var secureStorage: SecureStorage
    private lateinit var nearByService: NearbyService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authentication = FirebaseAuthentication()
        secureStorage = NotSecureStorage(this)
        nearByService = NearbyServiceProvider(this)
        nearByService.dataSendResultListener(dataSendResultListener)

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        email.error = null
        password.error = null

        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (emailStr.isEmpty()) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            authentication.addLoginResultListener(loginResultListener)
            authentication.login(FirebaseCredentials(emailStr, passwordStr))
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty() && password.length > 4
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            secureStorage.saveFirebaseCredentials(FirebaseCredentials(email.text.toString(), password.text.toString()))
            toMainActivity()
        }

        override fun onFailure(exception: Exception) {
            onFailure(exception)
        }

    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            val credentials = FirebaseCredentials(email.text.toString(), password.text.toString())
            val adapter = moshi.adapter(FirebaseCredentials::class.java)
            nearByService.sendData(adapter.toJson(credentials))
        }

        override fun failed(exception: Exception) {
            onFailure(exception)
        }
    }

    private fun onFailure(exception: Exception) {
        Timber.e(exception, "Request failed")
        password.error = getString(R.string.error_incorrect_password)
        password.requestFocus()
        showProgress(false)
    }

    private fun toMainActivity() {
        val intent = Intent(this, MobileActivity::class.java)
        startActivity(intent)
    }
}
