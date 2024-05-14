package com.kylecorry.trail_sense_comms.infrastructure.nearby

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.connection.bluetooth.IBluetoothDevice
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.BluetoothListener
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.SocketBluetoothDevice
import com.kylecorry.trail_sense_comms.ui.ConnectionDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// TODO: Attempt to reconnect when disconnected
// TODO: Support multiple devices (groups?)

class DeviceManager(private val context: Context) {

    var connectedDevice: IBluetoothDevice? = null
        private set
    var connected = false
        private set
    var isConnecting = false
        private set
    private val listener = BluetoothListener(context)

    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenJob: Job? = null
    private var onConnectionChanged: (() -> Unit)? = null

    fun start() {
        connectedDevice?.let { connect(it) }
        listenJob = scope.launch {
            listen()
        }
    }

    fun stop() {
        connectedDevice?.disconnect()
        connected = false
        isConnecting = false
        listenJob?.cancel()
    }

    fun onConnectionChanged(listener: () -> Unit) {
        onConnectionChanged = listener
    }

    @SuppressLint("MissingPermission")
    private suspend fun listen() {
        onIO {
            // TODO: Only keep listening when not connected to all devices in the chat
            while (true) {
                val socket = listener.listen("Trail Sense Comms", ConnectionDetails.bluetoothUUID)
                    ?: return@onIO
                println("Connected to ${socket.remoteDevice.name}")
                connect(
                    SocketBluetoothDevice(
                        context,
                        socket.remoteDevice.address,
                        socket
                    ) { it.createRfcommSocketToServiceRecord(ConnectionDetails.bluetoothUUID) }
                )
            }
        }
    }

    fun connect(device: IBluetoothDevice) {
        isConnecting = true
        onConnectionChanged?.invoke()
        try {
            connectedDevice?.disconnect()
            connectedDevice = device
            connectedDevice?.connect()
        } finally {
            isConnecting = false
            connected = connectedDevice?.isConnected() == true
            onConnectionChanged?.invoke()
        }
    }

}