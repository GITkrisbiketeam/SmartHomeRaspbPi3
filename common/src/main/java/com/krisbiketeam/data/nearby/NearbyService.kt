package com.krisbiketeam.data.nearby

//TODO: Add Stop/Pause/Resume
interface NearbyService {
    fun sendData(data: Any)
    fun dataSendResultListener(dataSendResultListener: DataSendResultListener)
    fun dataReceivedListener(dataReceiverListener: DataReceiverListener)
    fun isActive(): Boolean
    fun stop()
    fun pause()
    fun resume()

    interface DataSendResultListener {
        fun onSuccess()
        fun onFailure(exception: Exception)
    }

    interface DataReceiverListener {
        fun onDataReceived(data: ByteArray?)
    }
}
