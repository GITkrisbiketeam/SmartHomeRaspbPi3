package com.krisbiketeam.smarthomeraspbpi3.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.navigation.Navigation
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentSettingsWifiBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.WifiSettingsViewModel
import timber.log.Timber

//TODO: Add nearByService Stop/Pause on Lifecycle Pause/Stop
class WifiSettingsFragment : Fragment() {
    private lateinit var nearByService: NearbyService
    private lateinit var binding: FragmentSettingsWifiBinding

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            //secureStorage.saveFirebaseCredentials(FirebaseCredentials(ssid.text.toString(), password.text.toString()))
            toMainActivity()
        }

        override fun onFailure(exception: Exception) {
            onError(exception)
        }

    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        nearByService = NearbyServiceProvider(context!!)
        nearByService.dataSendResultListener(dataSendResultListener)

        val wifiSettingsViewModel = ViewModelProviders.of(this)
                .get(WifiSettingsViewModel::class.java)

        binding = DataBindingUtil.inflate<FragmentSettingsWifiBinding>(
                inflater, R.layout.fragment_settings_wifi, container, false).apply {
            viewModel = wifiSettingsViewModel

            password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptRemoteConnect()
                    return@OnEditorActionListener true
                }
                false
            })

            wifiConnectButton.setOnClickListener { attemptRemoteConnect() }
            setLifecycleOwner(this@WifiSettingsFragment)
        }

        return binding.root
    }

    private fun attemptRemoteConnect() {
        binding.ssid.error = null
        binding.password.error = null

        val ssidStr = binding.ssid.text.toString()
        val passwordStr = binding.password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            binding.password.error = getString(R.string.error_invalid_password)
            focusView = binding.password
            cancel = true
        }

        if (ssidStr.isEmpty()) {
            binding.ssid.error = getString(R.string.error_field_required)
            focusView = binding.ssid
            cancel = true
        } else if (!isSsidValid(ssidStr)) {
            binding.ssid.error = getString(R.string.error_invalid_email)
            focusView = binding.ssid
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
        return password.isNotEmpty() && password.length > 8
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        binding.wifiLoginForm.visibility = if (show) View.GONE else View.VISIBLE
        binding.wifiLoginForm.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.wifiLoginForm.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        binding.wifiProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.wifiProgress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.wifiProgress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    private fun onError(exception: Exception) {
        Timber.e(exception, "Request failed")
        binding.password.error = getString(R.string.error_incorrect_password)
        binding.password.requestFocus()
        showProgress(false)
    }


    private fun toMainActivity() {
        activity?.let {
            Navigation.findNavController(it, R.id.home_nav_fragment).navigateUp()
        }
    }

}
