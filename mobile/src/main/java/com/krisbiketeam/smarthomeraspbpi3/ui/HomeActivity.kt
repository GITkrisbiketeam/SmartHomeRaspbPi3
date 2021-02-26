package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.databinding.HomeActivityBinding
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

    private lateinit var binding: HomeActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate homeInformationRepository:$homeInformationRepository")
        binding = HomeActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        drawerLayout = binding.drawerLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.home_nav_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)

        // Set up ActionBar
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, drawerLayout)

        val currentUser = Firebase.auth.currentUser
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

        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            Timber.e("AuthStateListener  firebaseAuth:$firebaseAuth currentUser: ${firebaseAuth.currentUser?.email}")
        }

        homeInformationRepository.isUserOnline().observe(this) { userOnline ->
            Timber.d("isUserOnline  userOnline:$userOnline")
            binding.homeActivityConnectionProgress.visibility = if (userOnline == true) View.GONE else View.VISIBLE
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

    override fun onDestroy() {
        Timber.v("onDestroy")
        super.onDestroy()
    }

    override fun onResume() {
        Timber.v("onResume")
        if (!secureStorage.isAuthenticated()) {
            binding.homeActivityConnectionProgress.visibility = View.GONE
        }
        super.onResume()
    }

    override fun onPause() {
        Timber.v("onPause")
        if (secureStorage.isAuthenticated()) {
            binding.homeActivityConnectionProgress.visibility = View.VISIBLE
        }
        super.onPause()
    }
}