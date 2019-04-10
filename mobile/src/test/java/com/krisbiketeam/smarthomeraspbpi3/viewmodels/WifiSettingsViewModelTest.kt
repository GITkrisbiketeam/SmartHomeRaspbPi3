package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.di.testModule
import org.junit.*
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class WifiSettingsViewModelTest : KoinTest {

    private val viewModel: WifiSettingsViewModel by inject()

    @Mock
    lateinit var stateObserver: Observer<Pair<MyLiveDataState, Any>>

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
        Assert.assertEquals(viewModel.nearByState.value, Pair(MyLiveDataState.INIT, Unit))
    }

    @Test
    fun sendDataByNearbyService() {
        viewModel.nearByState.observeForever(stateObserver)
        viewModel.sendData(WifiCredentials("ssid", "password"))

        Mockito.verify(stateObserver)
                .onChanged(Pair(MyLiveDataState.CONNECTING, WifiCredentials("ssid", "password")))
    }
}