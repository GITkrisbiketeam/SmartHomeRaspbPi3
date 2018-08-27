package com.krisbiketeam.smarthomeraspbpi3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbyService
import kotlinx.android.synthetic.main.activity_wifi.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class WifiActivity : AppCompatActivity() {
    private val nearByService: NearbyService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi)

        nearByService.dataSendResultListener(dataSendResultListener)

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptRemoteConnect()
                return@OnEditorActionListener true
            }
            false
        })

        wifi_connect_button.setOnClickListener { attemptRemoteConnect() }
    }

    private fun attemptRemoteConnect() {
        ssid.error = null
        password.error = null

        val ssidStr = ssid.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (ssidStr.isEmpty()) {
            ssid.error = getString(R.string.error_field_required)
            focusView = ssid
            cancel = true
        } else if (!isSsidValid(ssidStr)) {
            ssid.error = getString(R.string.error_invalid_email)
            focusView = ssid
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            nearByService.sendData(WifiCredentials(ssidStr, passwordStr))
        }
    }

    private fun isSsidValid(email: String): Boolean {
        return true//email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty() && password.length > 4
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        wifi_form.visibility = if (show) View.GONE else View.VISIBLE
        wifi_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        wifi_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        wifi_progress.visibility = if (show) View.VISIBLE else View.GONE
        wifi_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        wifi_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            //secureStorage.saveFirebaseCredentials(FirebaseCredentials(ssid.text.toString(), password.text.toString()))
            toMainActivity()
        }

        override fun onFailure(exception: Exception) {
            onError(exception)
        }

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
