package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.krisbiketeam.data.MyLiveDataState
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.di.testModule
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
        Assert.assertEquals(viewModel.loginState.value, Pair(MyLiveDataState.INIT, Unit))
    }

    @Test
    fun sendDataByNearbyService() {
        viewModel.loginState.observeForever(stateObserver)
        //TODO: there is MediatorLiveData that should be somehow handled
        viewModel.login(FirebaseCredentials("email", "password"))

        Mockito.verify(stateObserver)
                .onChanged(Pair(MyLiveDataState.CONNECTING, WifiCredentials("email", "password")))
    }
}