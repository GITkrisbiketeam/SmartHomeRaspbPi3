package com.krisbiketeam.smarthomeraspbpi3.devicecontrols

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_ACTUATORS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_LIGHT_SWITCHES
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asPublisher
import org.koin.android.ext.android.inject
import org.reactivestreams.FlowAdapters
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.Flow
import java.util.function.Consumer

private const val CONTROL_REQUEST_CODE = 420
const val CONTROL_ID = "com.krisbiketeam.smarthomeraspbpi3.devicecontrols.ControlId"

@ExperimentalCoroutinesApi
@SuppressLint("NewApi")
class DeviceControlService : ControlsProviderService() {
    private val homeInformationRepository: FirebaseHomeInformationRepository by inject()
    private val secureStorage: SecureStorage by inject()

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && secureStorage.isAuthenticated() && secureStorage.homeName.isNotEmpty()) {
            Timber.d("onCreate initialize homeInformationRepository with proper Home and User data")
            homeInformationRepository.setHomeReference(secureStorage.homeName)
            homeInformationRepository.setUserReference(currentUser.uid)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
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
                    job = scope.launch {
                        combine(homeInformationRepository.homeUnitListFlow(HOME_LIGHT_SWITCHES), homeInformationRepository.homeUnitListFlow(HOME_ACTUATORS)) { lightSwitches, actuators ->
                            Timber.i("createPublisherForAllAvailable lightSwitches: ${lightSwitches.size} actuators: ${actuators.size}")
                            lightSwitches.forEach { homeUnit ->
                                flowSubscriber.onNext(getStatelessControl(homeUnit))
                            }
                            actuators.forEach { homeUnit ->
                                flowSubscriber.onNext(getStatelessControl(homeUnit))
                            }
                            flowSubscriber.onComplete()
                        }.collect()
                    }
                }
            })
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val controlsFlowList: List<kotlinx.coroutines.flow.Flow<Control>> = controlIds.map { controlId ->
            Timber.e("createPublisherFor controlId: $controlId")
            val (type, name) = controlId.getHomeUnitTypeAndName()
            homeInformationRepository.homeUnitFlow(type, name, true).distinctUntilChanged { old, new -> old.value == new.value }.map { homeUnit ->
                Timber.e("createPublisherFor homeUnitFlow homeUnit: $homeUnit")
                getStatefulControl(homeUnit)
            }.catch {
                Timber.e("createPublisherFor catch name: $name  it: $it")
                if (it is CancellationException) {
                    emit(getErrorStatefulControl(controlId, type, name))
                }
            }
        }
        return FlowAdapters.toFlowPublisher(flowOf(*controlsFlowList.toTypedArray()).flattenMerge().asPublisher())
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        Timber.e("performControlAction controlId: $controlId")
        val (type, name) = controlId.getHomeUnitTypeAndName()
        if (action is BooleanAction) {
            // Inform SystemUI that the action has been received and is being processed
            consumer.accept(ControlAction.RESPONSE_OK)
            Timber.e("performControlAction action.newState: ${action.newState}")
            homeInformationRepository.updateHomeUnitValue(type, name, action.newState)
        }
    }

    private fun getAppPendingIntent(controlId: String? = null): PendingIntent {
        return PendingIntent.getActivity(baseContext, CONTROL_REQUEST_CODE,
                Intent(baseContext, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (controlId != null) {
                        putExtra(CONTROL_ID, controlId)
                    }
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getStatelessControl(homeUnit: HomeUnit<Any>): Control {
        Timber.d(
                "getStatelessControl create Control for homeUnit: $homeUnit")
        val controlId = homeUnit.getControlId()
        return Control.StatelessBuilder(controlId, getAppPendingIntent(controlId))
                // Required: The name of the control
                .setTitle(homeUnit.name)
                // Required: Usually the room where the control is located
                .setSubtitle(homeUnit.hwUnitName?: "")
                // Optional: Structure where the control is located, an example would be a house
                //.setStructure(homeUnit.room)
                // Optional: Structure where the control is located, an example would be a house
                .setZone(homeUnit.room)
                // Required: Type of device, i.e., thermostat, light, switch
                .setDeviceType(homeUnit.getControlType())
                .build()
    }

    private fun getStatefulControl(homeUnit: HomeUnit<Any>, isError: Boolean = false): Control {
        Timber.d(
                "getStatefulControl create Control for homeUnit: $homeUnit")
        val controlId = homeUnit.getControlId()
        return Control.StatefulBuilder(controlId, getAppPendingIntent(controlId))
                // Required: The name of the control
                .setTitle(homeUnit.name)
                // Required: Usually the room where the control is located
                .setSubtitle(homeUnit.room)
                // Optional: Structure where the control is located, an example would be a house
                //.setStructure(homeUnit.room)
                // Optional: Structure where the control is located, an example would be a house
                .setZone(homeUnit.room)
                // Required: Type of device, i.e., thermostat, light, switch
                .setDeviceType(homeUnit.getControlType())
                .setStatus(Control.STATUS_OK)
                .setStatusText(if (homeUnit.value == true) "On" else "Off")
                .setControlTemplate(ToggleTemplate(homeUnit.getControlId(), ControlButton(homeUnit.value == true, "test")))
                .build()
    }

    private fun getErrorStatefulControl(controlId: String, type: String, name: String): Control {
        Timber.d(
                "getErrorStatefulControl create Error Control for type: $type name: $name")
        return Control.StatefulBuilder(controlId, getAppPendingIntent())
                // Required: The name of the control
                .setTitle(name)
                .setStatus(Control.STATUS_NOT_FOUND)
                .build()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun HomeUnit<Any>.getControlType(): Int {
    return when (this.type) {
        HOME_LIGHT_SWITCHES -> DeviceTypes.TYPE_LIGHT
        else -> DeviceTypes.TYPE_GENERIC_ON_OFF
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