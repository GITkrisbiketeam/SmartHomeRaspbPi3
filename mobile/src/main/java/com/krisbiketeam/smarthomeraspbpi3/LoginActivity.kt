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
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.storage.SecureStorage
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class LoginActivity : AppCompatActivity() {
    private val authentication: Authentication by inject()
    private val secureStorage: SecureStorage by inject()

    private val nearByService: NearbyService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        nearByService.dataSendResultListener(dataSendResultListener)

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        wifi_connect_button.setOnClickListener { attemptLogin() }
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            Timber.d("Firebase Login successful")
            nearByService.sendData(FirebaseCredentials(ssid.text.toString(), password.text.toString()))
            secureStorage.firebaseCredentials = FirebaseCredentials(ssid.text.toString(), password.text.toString())
        }

        override fun failed(exception: Exception) {
            Timber.d("Firebase Login fail")
            onError(exception)
        }
    }

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            Timber.d("Near credentials transfer success")
            toMainActivity()
        }

        override fun onFailure(exception: Exception) {
            Timber.d("Near credentials transfer fail")
            onError(exception)
        }

    }

    private fun attemptLogin() {
        //secureStorage.firebaseCredentials = FirebaseCredentials(email.text.toString(), password.text.toString())

        ssid.error = null
        password.error = null

        val emailStr = ssid.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (emailStr.isEmpty()) {
            ssid.error = getString(R.string.error_field_required)
            focusView = ssid
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            ssid.error = getString(R.string.error_invalid_email)
            focusView = ssid
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

    private fun onError(exception: Exception) {
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
