package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

@ExperimentalCoroutinesApi
fun getHwUnitsFlow(databaseReference: DatabaseReference?) = callbackFlow<Pair<ChildEventType, HwUnit>> {
    Timber.i("getHwUnitsFlow init on ${databaseReference?.toString()}")
    val eventListener = databaseReference?.addChildEventListener(object : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val hwUnit = dataSnapshot.getValue<HwUnit>()
            Timber.d("getHwUnitsFlow onChildAdded (key=$key)(hwUnit=$hwUnit)")
            hwUnit?.let {
                this@callbackFlow.trySendBlocking(ChildEventType.NODE_ACTION_ADDED to hwUnit)
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            val hwUnit = dataSnapshot.getValue<HwUnit>()
            Timber.d("getHwUnitsFlow onChildChanged (key=$key)(hwUnit=$hwUnit)")
            hwUnit?.let {
                this@callbackFlow.trySendBlocking(ChildEventType.NODE_ACTION_CHANGED to hwUnit)
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            val hwUnit = dataSnapshot.getValue<HwUnit>()
            Timber.d("getHwUnitsFlow onChildRemoved (key=$key)(hwUnit=$hwUnit)")
            hwUnit?.let {
                this@callbackFlow.trySendBlocking(ChildEventType.NODE_ACTION_DELETED to hwUnit)
            }
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue<HwUnit>()
            Timber.d("getHwUnitsFlow onChildMoved (key=$key)(room=$room)")
            //TODO does it also cover onChildChanged ??? or are those events both called???
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("getHwUnitsFlow  onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }
    })
    awaitClose {
        Timber.w("getHwUnitsFlow  awaitClose on ${databaseReference?.toString()}")
        eventListener?.run(databaseReference::removeEventListener)
    }
}.buffer(UNLIMITED)