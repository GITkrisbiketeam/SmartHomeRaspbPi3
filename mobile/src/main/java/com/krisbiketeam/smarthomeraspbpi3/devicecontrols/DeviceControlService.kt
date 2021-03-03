package com.krisbiketeam.smarthomeraspbpi3.devicecontrols

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_ACTUATORS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_LIGHT_SWITCHES
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asPublisher
import org.koin.android.ext.android.inject
import org.reactivestreams.FlowAdapters
import timber.log.Timber
import java.util.concurrent.Flow
import java.util.function.Consumer

private const val CONTROL_REQUEST_CODE = 420
const val CONTROL_ID = "com.krisbiketeam.smarthomeraspbpi3.devicecontrols.ControlId"

@SuppressLint("NewApi")
class DeviceControlService : ControlsProviderService() {
    private val homeInformationRepository: FirebaseHomeInformationRepository by inject()
    private val secureStorage: SecureStorage by inject()

    override fun onCreate() {
        super.onCreate()
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && secureStorage.isAuthenticated() && secureStorage.homeName.isNotEmpty()) {
            Timber.d("onCreate initialize homeInformationRepository with proper Home and User data")
            homeInformationRepository.setHomeReference(secureStorage.homeName)
            homeInformationRepository.setUserReference(currentUser.uid)
        }
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        return Flow.Publisher { flowSubscriber ->
            flowSubscriber.onSubscribe(object : Flow.Subscription {
                var job: Job? = null

                override fun cancel() {
                    Timber.w("createPublisherForAllAvailable cancel")
                    job?.cancel()
                }

                override fun request(count: Long) {
                    Timber.d("createPublisherForAllAvailable request $count")
                    job = GlobalScope.launch {
                        combine(homeInformationRepository.homeUnitListFlow(HOME_LIGHT_SWITCHES), homeInformationRepository.homeUnitListFlow(HOME_ACTUATORS)) { lightSwitches, actuators ->
                            Timber.i("createPublisherForAllAvailable collect HOME_LIGHT_SWITCHES")
                            lightSwitches.forEach { homeUnit ->
                                Timber.d(
                                        "createPublisherForAllAvailable create Control for homeUnit: $homeUnit")
                                val controlId = homeUnit.getControlId()
                                flowSubscriber.onNext(Control.StatelessBuilder(controlId, getAppPendingIntent(controlId))
                                        // Required: The name of the control
                                        .setTitle(homeUnit.name)
                                        // Required: Usually the room where the control is located
                                        .setSubtitle(homeUnit.hwUnitName)
                                        // Optional: Structure where the control is located, an example would be a house
                                        //.setStructure(homeUnit.room)
                                        // Optional: Structure where the control is located, an example would be a house
                                        .setZone(homeUnit.room)
                                        // Required: Type of device, i.e., thermostat, light, switch
                                        .setDeviceType(DeviceTypes.TYPE_LIGHT)
                                        .build())
                            }
                            Timber.i("createPublisherForAllAvailable collect HOME_ACTUATORS")
                            actuators.forEach { homeUnit ->
                                Timber.d(
                                        "createPublisherForAllAvailable create Control for homeUnit: $homeUnit")
                                val controlId = homeUnit.getControlId()
                                flowSubscriber.onNext(Control.StatelessBuilder(controlId, getAppPendingIntent(controlId))
                                        // Required: The name of the control
                                        .setTitle(homeUnit.name)
                                        // Required: Usually the room where the control is located
                                        .setSubtitle(homeUnit.hwUnitName)
                                        // Optional: Structure where the control is located, an example would be a house
                                        //.setStructure(homeUnit.room)
                                        // Optional: Structure where the control is located, an example would be a house
                                        .setZone(homeUnit.room)
                                        // Required: Type of device, i.e., thermostat, light, switch
                                        .setDeviceType(DeviceTypes.TYPE_GENERIC_ON_OFF)
                                        .build())
                            }

                            flowSubscriber.onComplete()
                        }.collect()
                    }
                }
            })
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val controlsFlowList: List<kotlinx.coroutines.flow.Flow<Control>> = controlIds.map {
            Timber.e("createPublisherFor controlId: $it")
            val (type, name) = it.getHomeUnitTypeAndName()
            homeInformationRepository.homeUnitFlow(type, name).distinctUntilChanged { old, new -> old.value == new.value }.map { homeUnit ->
                Timber.e("createPublisherFor homeUnitFlow homeUnit: $homeUnit")
                val controlId = homeUnit.getControlId()
                Control.StatefulBuilder(controlId, getAppPendingIntent(controlId))
                        // Required: The name of the control
                        .setTitle(homeUnit.name)
                        // Required: Usually the room where the control is located
                        .setSubtitle(homeUnit.room)
                        // Optional: Structure where the control is located, an example would be a house
                        //.setStructure(homeUnit.room)
                        // Optional: Structure where the control is located, an example would be a house
                        .setZone(homeUnit.room)
                        // Required: Type of device, i.e., thermostat, light, switch
                        .setDeviceType(if(type == HOME_LIGHT_SWITCHES) DeviceTypes.TYPE_LIGHT else DeviceTypes.TYPE_GENERIC_ON_OFF)
                        .setStatus(Control.STATUS_OK)
                        .setStatusText(if (homeUnit.value == true) "On" else "Off")
                        .setControlTemplate(ToggleTemplate(homeUnit.getControlId(), ControlButton(homeUnit.value == true, "test")))
                        .build()
            }
        }
        return FlowAdapters.toFlowPublisher(flowOf(*controlsFlowList.toTypedArray()).flattenMerge().asPublisher())
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        Timber.e("performControlAction controlId: $controlId")
        val (type, name) = controlId.getHomeUnitTypeAndName()
        if (action is BooleanAction) {
            Timber.e("performControlAction action.newState: ${action.newState}")
            homeInformationRepository.updateHomeUnitValue(type, name, action.newState)
        }
    }

    private fun getAppPendingIntent(controlId: String): PendingIntent {
        return PendingIntent.getActivity(baseContext, CONTROL_REQUEST_CODE,
                Intent(baseContext, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(CONTROL_ID, controlId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_CANCEL_CURRENT)
    }

}

fun HomeUnit<Any>.getControlId(): String {
    return this.type + '.' + this.name
}

fun String.getHomeUnitTypeAndName(): Pair<String, String> {
    val delimiterIdx = this.indexOfFirst { it == '.' }
    val type = this.substring(0, delimiterIdx)
    val name = this.substring(delimiterIdx + 1)
    return type to name
}