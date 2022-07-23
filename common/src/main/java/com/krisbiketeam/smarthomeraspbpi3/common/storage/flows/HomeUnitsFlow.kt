package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

@ExperimentalCoroutinesApi
fun getHomeUnitsFlow(homeNamePath: String?) = callbackFlow<Pair<ChildEventType, HomeUnit<Any>>> {
    val unitsList: List<MyChildEventListener> =
            homeNamePath?.let { homePath ->
                HOME_STORAGE_UNITS.map { storageUnit ->
                    MyChildEventListener(homePath, storageUnit, this)
                }
            } ?: emptyList()


    Timber.d("onActive")
    unitsList.forEach {
        it.reference.addChildEventListener(it)
    }

    awaitClose {
        Timber.e("getHwUnitsFlow  awaitClose on $homeNamePath")
        unitsList.forEach {
            it.reference.removeEventListener(it)
        }
    }
}.buffer(UNLIMITED)

class MyChildEventListener(homePath: String, private val storageUnit: String, private val sendChannel: SendChannel<Pair<ChildEventType, HomeUnit<Any>>>) : ChildEventListener {

    val reference = Firebase.database.getReference("$homePath/$HOME_UNITS_BASE/$storageUnit")

    override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
        // A new value has been added, add it to the displayed list
        val key = dataSnapshot.key
        homeUnitTypeIndicatorMap[storageUnit]?.run {
            val unit = try {
                dataSnapshot.getValue(this)
            } catch (e: DatabaseException) {
                Timber.e(e,"getHomeUnitsFlow onChildAdded (key=$key)(storageUnit=$storageUnit) could not get HomeUnit")
                null
            }
            Timber.d("getHomeUnitsFlow onChildAdded (key=$key)(unit=${unit?.name})")
            unit?.let {
                Timber.d("getHomeUnitsFlow onChildAdded (unit.room=${it.room})")
                // We need to create new SecureStorage unit as the one returned from GenericTypeIndicator is covariant
                sendChannel.trySendBlocking(ChildEventType.NODE_ACTION_ADDED to it.makeInvariant())
            }
        }
    }

    override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
        // A value has changed, use the key to determine if we are displaying this
        // value and if so displayed the changed value.
        val key = dataSnapshot.key
        homeUnitTypeIndicatorMap[storageUnit]?.run {
            val unit = try {
                dataSnapshot.getValue(this)
            } catch (e: DatabaseException) {
                Timber.e(e,"getHomeUnitsFlow onChildChanged (key=$key)(storageUnit=$storageUnit) could not get HomeUnit")
                null
            }
            Timber.d("getHomeUnitsFlow onChildChanged (key=$key)(unit=$unit)")
            unit?.let {
                Timber.d("getHomeUnitsFlow onChildChanged (unit.room=${it.room})")
                sendChannel.trySendBlocking(ChildEventType.NODE_ACTION_CHANGED to it.makeInvariant())
            }
        }
    }

    override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        Timber.d("getHomeUnitsFlow onChildRemoved: ${dataSnapshot.key}")

        // A value has changed, use the key to determine if we are displaying this
        // value and if so remove it.
        val key = dataSnapshot.key
        homeUnitTypeIndicatorMap[storageUnit]?.run {
            val unit = dataSnapshot.getValue(this)
            Timber.d("getHomeUnitsFlow onChildRemoved (key=$key)(unit=$unit)")
            unit?.let {
                Timber.d("getHomeUnitsFlow onChildRemoved (unit.room=${it.room})")
                sendChannel.trySendBlocking(ChildEventType.NODE_ACTION_DELETED to it.makeInvariant())
            }
        }
    }

    override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

        // A value has changed position, use the key to determine if we are
        // displaying this value and if so move it.
        val key = dataSnapshot.key
        homeUnitTypeIndicatorMap[storageUnit]?.run {
            val unit = dataSnapshot.getValue(this)
            //TODO does it also cover onChildChanged ??? or are those events both called???
            Timber.d("getHomeUnitsFlow onChildMoved (key=$key)(unit=$unit)")
        }
    }

    override fun onCancelled(databaseError: DatabaseError) {
        Timber.w("getHomeUnitsFlow onCancelled: $databaseError")
        sendChannel.close(databaseError.toException())
    }

}
