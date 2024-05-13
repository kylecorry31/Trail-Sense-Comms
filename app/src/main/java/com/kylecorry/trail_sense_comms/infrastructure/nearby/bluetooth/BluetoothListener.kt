package com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.kylecorry.andromeda.connection.bluetooth.BluetoothService
import com.kylecorry.luna.coroutines.onIO
import java.util.UUID

class BluetoothListener(context: Context) {

    private val bluetooth = BluetoothService(context)

    @SuppressLint("MissingPermission")
    suspend fun listen(name: String, uuid: UUID): BluetoothSocket? = onIO {
        val serverSocket = bluetooth.adapter?.listenUsingRfcommWithServiceRecord(name, uuid)
        serverSocket?.use { socket ->
            socket.accept()
        }
    }

}