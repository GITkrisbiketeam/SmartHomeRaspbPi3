package com.krisbiketeam.smarthomeraspbpi3.common.nearby

//TODO: Add Stop/Pause/Resume
@Deprecated("NearbyService should not be used anymore")
interface NearbyService {
    fun sendData(data: Any)
    fun dataSendResultListener(dataSendResultListener: DataSendResultListener)
    fun dataReceivedListener(dataReceiverListener: DataReceiverListener)
    fun isActive(): Boolean
    fun stop()
    fun pause()
    fun resume()

    @Deprecated("NearbyService should not be used anymore")
    interface DataSendResultListener {
        fun onSuccess()
        fun onFailure(exception: Exception)
    }

    @Deprecated("NearbyService should not be used anymore")
    interface DataReceiverListener {
        fun onDataReceived(data: ByteArray?)
    }
}
