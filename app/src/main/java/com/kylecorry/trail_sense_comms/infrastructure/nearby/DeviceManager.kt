package com.kylecorry.trail_sense_comms.infrastructure.nearby

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.connection.bluetooth.IBluetoothDevice
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.BluetoothListener
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.BluetoothNearbyDevice
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.SocketBluetoothDevice
import com.kylecorry.trail_sense_comms.ui.ConnectionDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// TODO: Attempt to reconnect when disconnected
// TODO: Support multiple devices (groups?)

class DeviceManager(private val context: Context) {

    var connectedDevice: NearbyDevice? = null
        private set
    val connected: Boolean
        get() = connectedDevice?.connectionStatus == ConnectionStatus.Connected
    val isConnecting: Boolean
        get() = connectedDevice?.connectionStatus == ConnectionStatus.Connecting
    private val listener = BluetoothListener(context)

    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenJob: Job? = null
    private var onConnectionChanged: (() -> Unit)? = null

    fun start() {
        connectedDevice?.let { connect(it) }
        // TODO: Stop listening when connected, restart when disconnected
        listenJob = scope.launch {
            listen()
        }
    }

    fun stop() {
        connectedDevice?.disconnect()
        listenJob?.cancel()
        listener.close()
    }

    fun onConnectionChanged(listener: () -> Unit) {
        onConnectionChanged = listener
    }

    @SuppressLint("MissingPermission")
    private suspend fun listen() {
        onIO {
            // TODO: Only keep listening when not connected to all devices in the chat
            while (true) {
                tryOrNothing {
                    val socket =
                        listener.listen("Trail Sense Comms", ConnectionDetails.bluetoothUUID)
                            ?: return@onIO
                    Log.d("DeviceManager", "Connected to ${socket.remoteDevice.name}")
                    connect(
                        BluetoothNearbyDevice(SocketBluetoothDevice(
                            context,
                            socket.remoteDevice.address,
                            socket
                        ) { it.createRfcommSocketToServiceRecord(ConnectionDetails.bluetoothUUID) }
                        ))
                }
            }
        }
    }

    // TODO: Do this in a coroutine
    fun connect(device: NearbyDevice) {
        onConnectionChanged?.invoke()
        try {
            connectedDevice?.disconnect()
            connectedDevice = device
            runBlocking {
                connectedDevice?.connect()
            }
        } finally {
            onConnectionChanged?.invoke()
        }
    }

}