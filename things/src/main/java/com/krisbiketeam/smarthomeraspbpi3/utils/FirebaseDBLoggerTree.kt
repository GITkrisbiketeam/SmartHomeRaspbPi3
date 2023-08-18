package com.krisbiketeam.smarthomeraspbpi3.utils

import android.util.Log
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object FirebaseDBLoggerTree : Timber.DebugTree() {
    private var minPriority: Int = Int.MAX_VALUE
    private var repository: FirebaseHomeInformationRepository? = null

    private val timeFormat = SimpleDateFormat("dd MMM HH:mm:ss.SSS", Locale.getDefault())

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val threadedTag = "[${Thread.currentThread().name}] $tag"
        val timeStamp = System.currentTimeMillis()
        val time = timeFormat.format(Date(timeStamp))
        val remoteLog = RemoteLog(priorityAsString(priority), threadedTag, message, t.toString(), time)
        repository?.logThingsLog(remoteLog, timeStamp)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return if (repository != null && minPriority <= priority) {
            super.isLoggable(tag, priority)
        } else {
            false
        }
    }

    // region public methods

    fun setFirebaseRepository(repository: FirebaseHomeInformationRepository) {
        this.repository = repository
    }

    // region public methods
    fun setMinPriority(minPriority: Int) {
        this.minPriority = minPriority
    }

    // endregion

    // region private methods

    private fun priorityAsString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> priority.toString()
    }

    // endregion
}

