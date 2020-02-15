package com.krisbiketeam.smarthomeraspbpi3.ui.settings

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import android.os.Bundle
import androidx.transition.TransitionManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.navigation.Navigation
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentSettingsLoginBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginSettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsLoginBinding
    private val loginSettingsViewModel by viewModel<LoginSettingsViewModel>()

    private val secureStorage: SecureStorage by inject()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate<FragmentSettingsLoginBinding>(inflater,
                                                                        R.layout.fragment_settings_login,
                                                                        container, false).apply {
            viewModel = loginSettingsViewModel

            password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin()
                    return@OnEditorActionListener true
                }
                false
            })
            loginSettingsViewModel.password.observe(viewLifecycleOwner, Observer {
                binding.passwordLayout.error = null
            })
            loginConnectButton.setOnClickListener { attemptLogin() }

            addOnRebindCallback(object : OnRebindCallback<ViewDataBinding>() {
                override fun onPreBind(binding: ViewDataBinding?): Boolean {
                    Timber.d("onPreBind")
                    binding?.let {
                        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
                    }
                    return super.onPreBind(binding)
                }
            })

            lifecycleOwner = this@LoginSettingsFragment
        }

        loginSettingsViewModel.loginState.observe(viewLifecycleOwner, Observer { pair ->
            pair?.let { (state, data) ->
                Timber.d("loginState changed state: $state data: $data")
                when (state) {
                    MyLiveDataState.ERROR      -> {
                        binding.passwordLayout.error = getString(R.string.error_incorrect_password)
                        binding.password.requestFocus()
                    }
                    MyLiveDataState.INIT       -> Unit
                    MyLiveDataState.CONNECTING -> Unit
                    MyLiveDataState.DONE       -> {
                        if (data is FirebaseCredentials) {
                            secureStorage.firebaseCredentials = data
                        }
                        activity?.let {
                            Navigation.findNavController(it, R.id.home_nav_fragment).navigateUp()
                        }
                    }
                }
            }
        })

        return binding.root
    }

    private fun attemptLogin() {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        val emailStr = binding.email.text.toString()
        val passwordStr = binding.password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            binding.passwordLayout.error = getString(R.string.error_invalid_password)
            focusView = binding.password
            cancel = true
        }

        if (emailStr.isEmpty()) {
            binding.emailLayout.error = getString(R.string.error_field_required)
            focusView = binding.email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            focusView = binding.email
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            loginSettingsViewModel.login(FirebaseCredentials(emailStr, passwordStr))
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty() && password.length > 8
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }
}
