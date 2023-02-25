package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import timber.log.Timber
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
inline fun <reified T> genericListReferenceFlow(databaseReference: DatabaseReference?, closeOnEmpty: Boolean = false) = callbackFlow<List<T>> {
    Timber.d("genericListReferenceFlow init on ${databaseReference?.toString()}")
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericListReferenceFlow  onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (closeOnEmpty && !dataSnapshot.exists()) {
                this@callbackFlow.close(CancellationException("Data does not exists ${databaseReference.key}"))
                return
            }
            // A new value has been added, add it to the displayed list
            val list = dataSnapshot.children.mapNotNull {
                it.getValue<T>()
            }
            //Timber.e("genericListReferenceFlow onDataChange (key=${dataSnapshot.key})(homeUnits=$list)")
            this@callbackFlow.trySendBlocking(list)
        }
    })
    awaitClose {
        Timber.e("genericListReferenceFlow  awaitClose on ${databaseReference?.toString()}")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()

@ExperimentalCoroutinesApi
inline fun <reified T> genericMapReferenceFlow(databaseReference: DatabaseReference?, closeOnEmpty: Boolean = false) = callbackFlow<Map<String, T>> {
    Timber.d("genericMapReferenceFlow init on ${databaseReference?.toString()}")
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericMapReferenceFlow onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (closeOnEmpty && !dataSnapshot.exists()) {
                this@callbackFlow.close(CancellationException("Data does not exists ${databaseReference.key}"))
                return
            }
            // A new value has been added, add it to the displayed list
            val list: HashMap<String, T> = HashMap()
            for (child: DataSnapshot in dataSnapshot.children) {
                val key: String? = child.key
                val value = child.getValue<T>()
                if (value != null && key != null) {
                    list[key] = value
                }
            }
            //Timber.e("genericMapReferenceFlow onDataChange (key=${dataSnapshot.key})(homeUnits=$list)")
            this@callbackFlow.trySendBlocking(list)
        }
    })
    awaitClose {
        Timber.e("genericMapReferenceFlow  awaitClose on ${databaseReference?.toString()}")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()

@ExperimentalCoroutinesApi
inline fun <reified T> genericReferenceFlow(databaseReference: DatabaseReference?, closeOnEmpty: Boolean = false) = callbackFlow<T> {
    Timber.d("genericReferenceFlow init on ${databaseReference?.toString()}")
    val eventListener = databaseReference?.addValueEventListener(object : ValueEventListener {
        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("genericReferenceFlow  onCancelled $databaseError")
            this@callbackFlow.close(databaseError.toException())
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (closeOnEmpty && !dataSnapshot.exists()) {
                this@callbackFlow.close(CancellationException("Data does not exists ${databaseReference.key}"))
                return
            }
            // A new value has been added, add it to the displayed list
            val value: T? = dataSnapshot.getValue<T>()
            Timber.e("genericReferenceFlow onDataChange (key=${dataSnapshot.key})(value=$value) exists: ${dataSnapshot.exists()}")
            if (value != null) {
                this@callbackFlow.trySendBlocking(value)
            }
        }
    })
    awaitClose {
        Timber.e("genericReferenceFlow  awaitClose on ${databaseReference?.toString()}")
        eventListener?.let { eventListener ->
            databaseReference.removeEventListener(eventListener)
        }
    }
}.conflate()
