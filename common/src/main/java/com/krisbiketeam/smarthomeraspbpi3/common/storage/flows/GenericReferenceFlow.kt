package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import timber.log.Timber

@ExperimentalCoroutinesApi
inline fun <reified T> genericListReferenceFlow(databaseReference: DatabaseReference?) = callbackFlow<List<T>> {
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericListReferenceFlow  onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val list: ArrayList<T> = ArrayList()
            for (child: DataSnapshot in dataSnapshot.children) {
                val value = child.getValue<T>()
                value?.run {
                    list.add(value)
                }
            }
            Timber.e("genericListReferenceFlow onDataChange (key=${dataSnapshot.key})(homeUnits=$list)")
            this@callbackFlow.sendBlocking(list)
        }
    })
    awaitClose {
        Timber.e("genericReferenceFlow  awaitClose")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()

@ExperimentalCoroutinesApi
inline fun <reified T> genericMapReferenceFlow(databaseReference: DatabaseReference?) = callbackFlow<Map<String, T>> {
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericMapReferenceFlow onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val list: HashMap<String, T> = HashMap()
            for (child: DataSnapshot in dataSnapshot.children) {
                val key: String? = child.key
                val value = child.getValue<T>()
                if (value != null && key != null) {
                    list[key] = value
                }
            }
            Timber.e("genericMapReferenceFlow onDataChange (key=${dataSnapshot.key})(homeUnits=$list)")
            this@callbackFlow.sendBlocking(list)
        }
    })
    awaitClose {
        Timber.e("genericReferenceFlow  awaitClose")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()

@ExperimentalCoroutinesApi
inline fun <reified T> genericReferenceFlow(databaseReference: DatabaseReference?) = callbackFlow<T> {
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericReferenceFlow  onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val value: T? = dataSnapshot.getValue<T>()
            Timber.e("genericReferenceFlow onDataChange (key=${dataSnapshot.key})(value=$value)")
            if (value != null) {
                this@callbackFlow.sendBlocking(value)
            }
        }
    })
    awaitClose {
        Timber.e("genericReferenceFlow  awaitClose")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()
