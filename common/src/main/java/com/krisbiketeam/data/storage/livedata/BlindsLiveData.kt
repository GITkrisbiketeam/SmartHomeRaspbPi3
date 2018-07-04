package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.Blind
import timber.log.Timber

class BlindsLiveData(private val databaseReference: DatabaseReference) : LiveData<Blind>() {
    private val childEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new blind has been added, add it to the displayed list
            val blind = dataSnapshot.getValue(Blind::class.java)
            val blindKey = dataSnapshot.getKey()

            Timber.d("onChildChanged (temperatureKey=$blindKey) (value=$value) (newValue=$blind) ")
            value = blind
        }

        override  fun onChildChanged(dataSnapshot: DataSnapshot , previousChildName: String?) {
            // A blind has changed, use the key to determine if we are displaying this
            // blind and if so displayed the changed blind.
            val blind = dataSnapshot.getValue(Blind::class.java)
            val blindKey = dataSnapshot.getKey()
            Timber.d("onChildChanged (temperatureKey=$blindKey) (value=$value) (newValue=$blind)")
            value = blind
        }

        override  fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.getKey());

            // A blind has changed, use the key to determine if we are displaying this
            // blind and if so remove it.
            val blindKey = dataSnapshot.getKey()

            // ...
        }

        override  fun onChildMoved(dataSnapshot: DataSnapshot , previousChildName: String?) {
            Timber.d("onChildMoved:" + dataSnapshot.getKey());

            // A blind has changed position, use the key to determine if we are
            // displaying this blind and if so move it.
            val blind = dataSnapshot.getValue(Blind::class.java)
            val blindKey = dataSnapshot.getKey()

            // ...
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled:", databaseError);

        }
    }

    override fun onActive() {
        databaseReference.addChildEventListener(childEventListener)
    }

    override fun onInactive() {
        databaseReference.removeEventListener(childEventListener)
    }
}
