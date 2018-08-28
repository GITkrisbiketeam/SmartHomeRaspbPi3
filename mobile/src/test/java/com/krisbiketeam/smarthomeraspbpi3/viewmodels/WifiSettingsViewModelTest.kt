package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.data.nearby.NearbySettingsState
import com.krisbiketeam.smarthomeraspbpi3.di.testModule
import org.junit.*
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class WifiSettingsViewModelTest : KoinTest {

    val viewModel: WifiSettingsViewModel by inject()

    @Mock
    lateinit var stateObserver: Observer<Pair<NearbySettingsState, Any>>

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
        Assert.assertEquals(viewModel.nearByState.value, Pair(NearbySettingsState.INIT, Unit))
    }

    @Test
    fun sendDataByNearbyService() {
        viewModel.nearByState.observeForever(stateObserver)
        viewModel.sendData(WifiCredentials("email", "password"))

        Mockito.verify(stateObserver)
                .onChanged(Pair(NearbySettingsState.CONNECTING, WifiCredentials("email", "password")))
    }
}