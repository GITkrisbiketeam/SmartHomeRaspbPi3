package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.NavHomeDirections
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.databinding.HomeActivityBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.NavHeaderBinding
import com.krisbiketeam.smarthomeraspbpi3.devicecontrols.CONTROL_ID
import com.krisbiketeam.smarthomeraspbpi3.devicecontrols.getHomeUnitTypeAndName
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NavigationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@ExperimentalCoroutinesApi
class HomeActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val homeInformationRepository: FirebaseHomeInformationRepository by inject()
    private val secureStorage: SecureStorage by inject()

    private val navigationViewModel by viewModel<NavigationViewModel>()

    private lateinit var binding: HomeActivityBinding

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate homeInformationRepository:$homeInformationRepository")
        binding = HomeActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        drawerLayout = binding.drawerLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.home_nav_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)

        // Set up ActionBar
        setSupportActionBar(binding.toolbar)

        appBarConfiguration = AppBarConfiguration(setOf(R.id.room_list_fragment, R.id.task_list_fragment, R.id.room_detail_fragment), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up BottomBar
        binding.bottomNavigation.setupWithNavController(navController)

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null || !secureStorage.isAuthenticated()) {
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(NavHomeDirections.goToLoginSettingsFragment())
        } else if (secureStorage.homeName.isEmpty()) {
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(NavHomeDirections.goToHomeSettingsFragment())
        } else {
            homeInformationRepository.setHomeReference(secureStorage.homeName)
            homeInformationRepository.setUserReference(currentUser.uid)
        }

        val controlId = intent.extras?.getString(CONTROL_ID)
        if (controlId != null) {
            val (type, name) = controlId.getHomeUnitTypeAndName()
            Timber.e("onCreate controlId:$controlId")
            navController.navigate(NavHomeDirections.goToHomeUnitDetailFragment("", name, type))
        }
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            Timber.e("AuthStateListener  firebaseAuth:$firebaseAuth currentUser: ${firebaseAuth.currentUser?.email}")
        }

        lifecycleScope.launch {
            homeInformationRepository.isUserOnlineFlow()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect { userOnline ->
                        Timber.d("isUserOnline  userOnline:$userOnline")
                        binding.homeActivityConnectionProgress.visibility = if (userOnline == true) View.GONE else View.VISIBLE
                    }
        }

        DataBindingUtil.inflate<NavHeaderBinding>(layoutInflater, R.layout.nav_header,
                binding.navigationView, false).apply {
            binding.navigationView.addHeaderView(root)
            viewModel = navigationViewModel
            lifecycleOwner = this@HomeActivity
        }

        fillNavigationViewDrawer()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
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

    private fun fillNavigationViewDrawer() {
        lifecycleScope.launch {
            navigationViewModel.roomListMenu
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect { roomList ->
                        Timber.d("roomList :$roomList")
                        binding.navigationView.menu.removeGroup(R.id.room_list_fragment)
                        if (roomList.isNullOrEmpty()) {
                            binding.navigationView.menu.add(R.id.room_list_fragment,
                                    R.id.room_list_fragment,
                                    Menu.FIRST,
                                    R.string.new_room_dialog_title)
                                    .setOnMenuItemClickListener {
                                        navController.navigate(RoomListFragmentDirections.actionRoomListFragmentToNewRoomDialogFragment())
                                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                            drawerLayout.closeDrawers()
                                        }
                                        true
                                    }.setIcon(R.drawable.ic_baseline_add_box_24)
                        } else {
                            binding.navigationView.menu.add(R.id.room_list_fragment,
                                    R.id.room_list_fragment,
                                    Menu.FIRST,
                                    R.string.menu_navigation_room_list)
                                    .setIcon(R.drawable.ic_baseline_other_houses_24)
                                    .setCheckable(true)
                                    .setChecked(true)

                            roomList.forEachIndexed { index, room ->
                                binding.navigationView.menu.add(R.id.room_list_fragment,
                                        R.id.home_unit_detail_fragment,
                                        index + 1,
                                        "\t\t${room.name}")
                                        .setOnMenuItemClickListener {
                                            navController.navigate(RoomListFragmentDirections.goToRoomFragment(room.name))
                                            it.isChecked = true
                                            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                                drawerLayout.closeDrawers()
                                            }
                                            true
                                        }.setIcon(R.drawable.ic_outline_label_24)
                                        .setCheckable(true)
                            }
                        }
                    }
        }
        binding.navigationView.menu.add(Menu.NONE,
                R.id.logs_fragment,
                Menu.CATEGORY_SECONDARY + 1,
                R.string.menu_navigation_logs)
                .setIcon(R.drawable.ic_baseline_view_headline_24)
        binding.navigationView.menu.add(Menu.NONE,
                R.id.settings_fragment,
                Menu.CATEGORY_SECONDARY + 2,
                R.string.menu_navigation_settings)
                .setIcon(R.drawable.ic_baseline_settings_24)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.room_detail_fragment) {
                for (menuItem in binding.navigationView.menu.iterator()) {
                    if (menuItem.title.contains(arguments?.get("roomName").toString())) {
                        menuItem.setChecked(true)
                        break
                    }

                }
            }
        }
    }
}