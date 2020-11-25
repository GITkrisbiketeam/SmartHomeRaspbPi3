package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.NavHeaderBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NavigationViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val homeInformationRepository: FirebaseHomeInformationRepository by inject()
    private val secureStorage: SecureStorage by inject()

    private val navigationViewModel by viewModel<NavigationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityHomeBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_home)
        drawerLayout = binding.drawerLayout

        val navController = findNavController(R.id.home_nav_fragment)


        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)

        // Set up ActionBar
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, drawerLayout)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || !secureStorage.isAuthenticated()) {
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(R.id.login_settings_fragment)
        } else if (secureStorage.homeName.isEmpty()) {
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(R.id.home_settings_fragment)
        } else {
            homeInformationRepository.setHomeReference(secureStorage.homeName)
            homeInformationRepository.setUserReference(currentUser.uid)
        }

        DataBindingUtil.inflate<NavHeaderBinding>(layoutInflater, R.layout.nav_header,
                                                  binding.navigationView, false).apply {
            binding.navigationView.addHeaderView(root)
            viewModel = navigationViewModel
            lifecycleOwner = this@HomeActivity
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(Navigation.findNavController(this, R.id.home_nav_fragment),
                                       drawerLayout)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}