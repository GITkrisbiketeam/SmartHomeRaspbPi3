package com.krisbiketeam.smarthomeraspbpi3.common.storage.flows

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.getHomeUnitTypeIndicatorMap
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_UNITS_BASE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
fun getHomeUnitListFlow(homeNamePath: String?, unitType: HomeUnitType): Flow<List<HomeUnit<Any>>> {
    return homeNamePath?.let { home ->
        if (unitType != HomeUnitType.UNKNOWN) {
            Firebase.database.getReference("$home/$HOME_UNITS_BASE/$unitType").let { reference ->
                genericListReferenceFlow(reference, unitType)
            }
        } else {
            combine(HOME_STORAGE_UNITS.map { type ->
                Firebase.database.getReference("$home/$HOME_UNITS_BASE/$type").let { reference ->
                    genericListReferenceFlow(reference, type)
                }
            }) { types ->
                types.flatMap { it }
            }
        }
    } ?: emptyFlow()
}

@ExperimentalCoroutinesApi
private fun genericListReferenceFlow(
    databaseReference: DatabaseReference?,
    storageUnitType: HomeUnitType,
    closeOnEmpty: Boolean = false
) = callbackFlow<List<HomeUnit<Any>>> {
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
                try {
                    it.getValue(getHomeUnitTypeIndicatorMap(storageUnitType))
                } catch (e: DatabaseException) {
                    Timber.e(
                        e,
                        "getHomeUnitListFlow error (dataSnapshot=$it)(storageUnitType=$storageUnitType) could not get HomeUnit"
                    )
                    null
                }
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
