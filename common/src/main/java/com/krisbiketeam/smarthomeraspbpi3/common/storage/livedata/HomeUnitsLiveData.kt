package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import timber.log.Timber

class HomeUnitsLiveData(private val homeNamePath: String?) :
        LiveData<Pair<ChildEventType, HomeUnit<Any?>>>() {

    private val unitsList: List<Pair<DatabaseReference, MyChildEventListener>> by lazy {
        homeNamePath?.let { homePath ->
            HOME_STORAGE_UNITS.map { storageUnit ->
                MyChildEventListener(storageUnit).let { childListener ->
                    FirebaseDatabase.getInstance()
                            .getReference("$homePath/$HOME_UNITS_BASE/${childListener.childNode}") to
                            childListener
                }
            }
        } ?: emptyList()
    }

    private val typeIndicatorMap: HashMap<String, GenericTypeIndicator<out HomeUnit<out Any?>>> by lazy {
        hashMapOf(HOME_LIGHTS to object : GenericTypeIndicator<HomeUnit<LightType?>>() {},
                HOME_ACTUATORS to object : GenericTypeIndicator<HomeUnit<ActuatorType?>>() {},
                HOME_LIGHT_SWITCHES to object :
                        GenericTypeIndicator<HomeUnit<LightSwitchType?>>() {},
                HOME_REED_SWITCHES to object :
                        GenericTypeIndicator<HomeUnit<ReedSwitchType?>>() {},
                HOME_MOTIONS to object : GenericTypeIndicator<HomeUnit<MotionType?>>() {},
                HOME_TEMPERATURES to object :
                        GenericTypeIndicator<HomeUnit<TemperatureType?>>() {},
                HOME_PRESSURES to object : GenericTypeIndicator<HomeUnit<PressureType?>>() {},
                HOME_HUMIDITY to object : GenericTypeIndicator<HomeUnit<HumidityType?>>() {},
                HOME_BLINDS to object : GenericTypeIndicator<HomeUnit<BlindType?>>() {})
    }

    inner class MyChildEventListener(val childNode: String) : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = try {
                    dataSnapshot.getValue(this)
                } catch (e: DatabaseException) {
                    Timber.e("onChildAdded (key=$key)(childNode=$childNode) could not get HomeUnit")
                    null
                }
                Timber.d("onChildAdded (key=$key)(unit=${unit?.name})")
                unit?.let {
                    Timber.d("onChildAdded (unit.room=${it.room})")
                    // We need to create new SecureStorage unit as the one returned from GenericTypeIndicator is covariant
                    value = ChildEventType.NODE_ACTION_ADDED to it.makeInvariant()
                }
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                Timber.d("onChildChanged (key=$key)(unit=$unit)")
                unit?.let {
                    Timber.d("onChildChanged (unit.room=${it.room})")
                    value = ChildEventType.NODE_ACTION_CHANGED to it.makeInvariant()
                }
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved: ${dataSnapshot.key}")

            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                Timber.d("onChildRemoved (key=$key)(unit=$unit)")
                unit?.let {
                    Timber.d("onChildRemoved (unit.room=${it.room})")
                    value = ChildEventType.NODE_ACTION_DELETED to it.makeInvariant()
                }
            }
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                //TODO does it also cover onChildChanged ??? or are those events both called???
                Timber.d("onChildMoved (key=$key)(unit=$unit)")
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.w("onCancelled: $databaseError")
        }

    }

    override fun onActive() {
        Timber.d("onActive")
        unitsList.forEach {
            it.first.addChildEventListener(it.second)
        }
    }

    override fun onInactive() {
        Timber.d("onInactive")
        unitsList.forEach {
            it.first.removeEventListener(it.second)
        }
    }
}
