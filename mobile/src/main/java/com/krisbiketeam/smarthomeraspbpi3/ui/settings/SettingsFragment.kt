package com.krisbiketeam.smarthomeraspbpi3.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * A fragment representing settings screen.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private val analytics: Analytics by inject()

    private val homeRepository: FirebaseHomeInformationRepository by inject()

    private val secureStorage: SecureStorage by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_fragment_preference, rootKey)

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                homeRepository.restartAppFlow().collect { restart ->
                    if (restart) {
                        findPreference<Preference>(getString(R.string.settings_restart_rpi_things_app))?.summary =
                            getString(R.string.settings_restarting)
                    }
                }
            }
        }

        // TODO: This will not be secure
        val alarmSwitch:SwitchPreferenceCompat? = findPreference(resources
                .getString(R.string.settings_alarm_enabled_key))
        alarmSwitch?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                secureStorage.alarmEnabled = newValue
                true
            } else {
                false
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        Timber.d("onPreferenceTreeClick preference: $preference")
        when (preference.key) {
            getString(R.string.settings_login_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to LoginSettings")
                val direction =
                        SettingsFragmentDirections.actionSettingsFragmentToLoginSettingsFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_home_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to HomeSettings")
                val direction =
                        SettingsFragmentDirections.actionSettingsFragmentToHomeSettingsFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_hw_unit_list_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to HwUnitList")
                val direction =
                        SettingsFragmentDirections.actionSettingsFragmentToHwUnitListFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_hw_unit_error_event_list_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to HwUnitErrorEventList")
                val direction =
                        SettingsFragmentDirections.actionSettingsFragmentToHwUnitErrorEventListFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_things_app_logs_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to ThingsAppLogsFragment")
                val direction =
                    SettingsFragmentDirections.actionSettingsFragmentToThingsAppLogsFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_hw_unit_error_logs_fragment_key) -> {
                Timber.d("onPreferenceTreeClick go to HwUnitErrorLogsFragment")
                val direction =
                    SettingsFragmentDirections.actionSettingsFragmentToHwUnitErrorLogsFragment()
                findNavController().navigate(direction)
                return true
            }
            getString(R.string.settings_restart_rpi_things_app) -> {
                Timber.d("onPreferenceTreeClick restart Rpi Things App")
                homeRepository.setResetAppFlag()
                return true
            }
            else -> {
                return super.onPreferenceTreeClick(preference)
            }
        }
    }
}
