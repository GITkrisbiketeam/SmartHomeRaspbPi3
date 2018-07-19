package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.Exclude

data class StorageUnit<T>(var name: String = "",
                          var room: String = "",
                          var unitName: String = "",
                          var firebaseTableName: String = "",
                          var value: T? = null,
                          val unitsTasks: MutableList<UnitTask> = ArrayList()) {

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: StorageUnit<T>.(T) -> Unit = { Unit }
}