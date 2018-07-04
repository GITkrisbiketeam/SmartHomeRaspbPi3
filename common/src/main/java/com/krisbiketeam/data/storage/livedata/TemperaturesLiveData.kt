package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.krisbiketeam.data.storage.dto.Temperature
import timber.log.Timber


class TemperaturesLiveData(private val databaseReference: DatabaseReference) : LiveData<Temperature>() {

    private val childEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new temperature has been added, add it to the displayed list
            val temperature = dataSnapshot.getValue(Temperature::class.java)
            val temperatureKey = dataSnapshot.getKey()

            Timber.d("onChildChanged (temperatureKey=$temperatureKey) (newValue=$temperature) (oldValue=$value) ")
            value = temperature
        }

        override  fun onChildChanged(dataSnapshot: DataSnapshot , previousChildName: String?) {
            // A temperature has changed, use the key to determine if we are displaying this
            // temperature and if so displayed the changed temperature.
            val temperature = dataSnapshot.getValue(Temperature::class.java)
            val temperatureKey = dataSnapshot.getKey()
            Timber.d("onChildChanged (temperatureKey=$temperatureKey) (newValue=$temperature) (oldValue=$value) ")
            value = temperature
        }

        override  fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.getKey());

            // A temperature has changed, use the key to determine if we are displaying this
            // temperature and if so remove it.
            val temperatureKey = dataSnapshot.getKey()

            // ...
        }

        override  fun onChildMoved(dataSnapshot: DataSnapshot , previousChildName: String?) {
            Timber.d("onChildMoved:" + dataSnapshot.getKey());

            // A temperature has changed position, use the key to determine if we are
            // displaying this temperature and if so move it.
            val temperature = dataSnapshot.getValue(Temperature::class.java)
            val temperatureKey = dataSnapshot.getKey()

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
