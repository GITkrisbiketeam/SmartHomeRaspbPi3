package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.RemoteConnectionState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.di.testModule
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import org.junit.*
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class LoginSettingsViewModelTest : KoinTest {

    private val viewModel: LoginSettingsViewModel by inject()

    @Mock
    lateinit var stateObserver: Observer<Pair<RemoteConnectionState, Any>>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        StandAloneContext.startKoin(listOf(testModule))
    }

    @After
    fun after() {
        StandAloneContext.closeKoin()
    }

    @Test
    fun initStateCorrect() {
        Assert.assertEquals(viewModel.loginState.value, Pair(RemoteConnectionState.INIT, Unit))
    }

    @Test
    fun sendDataByNearbyService() {
        viewModel.loginState.observeForever(stateObserver)
        viewModel.login(FirebaseCredentials("email", "password"))

        Mockito.verify(stateObserver)
                .onChanged(Pair(RemoteConnectionState.CONNECTING, WifiCredentials("email", "password")))
    }
}