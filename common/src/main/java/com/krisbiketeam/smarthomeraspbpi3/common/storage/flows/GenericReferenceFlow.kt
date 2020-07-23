package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import timber.log.Timber

@ExperimentalCoroutinesApi
inline fun <reified T> genericListReferenceFlow(databaseReference: DatabaseReference?) = callbackFlow<List<T>> {
    val typeIndicator = object : GenericTypeIndicator<T>() {}
    databaseReference?.let { reference ->
        val eventListener = reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                this@callbackFlow.close(databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val list: ArrayList<T> = ArrayList()
                for (child: DataSnapshot in dataSnapshot.children) {
                    val homeUnit = child.getValue(typeIndicator)
                    homeUnit?.run {
                        list.add(homeUnit)
                    }
                }
                Timber.e("onDataChange (key=${dataSnapshot.key})(homeUnits=$list)")
                this@callbackFlow.sendBlocking(list)
            }
        })
        awaitClose {
            reference.removeEventListener(eventListener)
        }
    }

}.conflate()
