package com.kylecorry.trail_sense_comms.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.connection.bluetooth.BluetoothScanner
import com.kylecorry.andromeda.connection.bluetooth.BluetoothService
import com.kylecorry.andromeda.connection.bluetooth.IBluetoothDevice
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense_comms.R
import com.kylecorry.trail_sense_comms.databinding.FragmentMainBinding
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.BluetoothListener
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.SocketBluetoothDevice
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@AndroidEntryPoint
class MainFragment : BoundFragment<FragmentMainBinding>() {

    private val bluetooth by lazy { BluetoothService(requireContext()) }

    // TODO: Support multiple devices (groups?)
    // TODO: Try out just in time connection (don't connect until message is sent)
    private var connectedDevice by state<IBluetoothDevice?>(null)
    private var connected by state(false)
    private var devices by state(emptyList<BluetoothDevice>())
    private val listener by lazy { BluetoothListener(requireContext()) }
    private var messages by state(emptyList<Message>())
    private val scanner by lazy { BluetoothScanner(requireContext()) }

    // TODO: Is this the right thing to do?
    private val uuid = UUID.fromString("f237b0ff-ef4c-43c9-a476-44508cb26e58")

    // TODO: Attempt to reconnect when disconnected

    // TODO: Run in background

    // TODO: Show connection status of all devices in the group

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Handle permissions
        if (
            !Permissions.hasPermission(requireContext(), Manifest.permission.BLUETOOTH) ||
            !Permissions.hasPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) ||
            !Permissions.hasPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) ||
            !Permissions.hasPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) ||
            !Permissions.hasPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE)
        ){
            return
        }


        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            if (text.isBlank()) {
                return@setOnClickListener
            }
            inBackground {
                connectedDevice?.let { sendText(text, it) }
            }
            binding.messageInput.setText("")

            messages = messages + Message(
                "Me", // TODO: Get my name
                text,
                Instant.now(),
                true
            )

            // TODO: Show send status
        }

        // TODO: Enable scanner / move this out of the fragment
        devices = bluetooth.bondedDevices
//        observe(scanner) {
//            devices = (scanner.devices + bluetooth.bondedDevices).distinctBy { it.address }
//        }

        // Start listener
        inBackground {
            onIO {
                // TODO: Only keep listening when not connected to all devices in the chat
                while (true) {
                    val socket = listener.listen("Test", uuid) ?: return@onIO
                    println("Connected to ${socket.remoteDevice.name}")
                    connect(
                        SocketBluetoothDevice(
                            requireContext(),
                            socket.remoteDevice.address,
                            socket
                        ) { it.createRfcommSocketToServiceRecord(uuid) }
                    )
                }
            }
        }

        inBackground {
            onIO {
                while (true) {
                    // TODO: Handle disconnect
                    // TODO: Handle audio streams
                    // TODO: Handle files / images
                    val device = connectedDevice ?: continue
                    val message = receiveMessage(device) ?: continue
                    messages = messages + message
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        connectedDevice?.connect()
    }

    override fun onPause() {
        super.onPause()
        connectedDevice?.disconnect()
    }

    private fun connect(device: IBluetoothDevice) {
        connectedDevice?.disconnect()
        connectedDevice = device
        connectedDevice?.connect()
        connected = connectedDevice?.isConnected() == true
    }

    @SuppressLint("MissingPermission")
    override fun onUpdate() {
        super.onUpdate()
        effect("connection", connectedDevice?.name, connected) {
            binding.toolbar.subtitle.text =
                if (connected) "Connected to ${connectedDevice?.name}" else "Disconnected"
        }

        effect("devices", devices) {
            binding.list.setItems(devices.filter { it.name != null }.mapIndexed { index, device ->
                ListItem(index.toLong(), device.name ?: "??", device.address) {
                    println("Connecting to ${device.name}")
                    connect(bluetooth.getSecureDevice(device.address, uuid))
                }
            })
        }

        effect("messages", messages) {
            binding.messages.setItems(messages.mapIndexed { index, message ->
                val time = message.time.toZonedDateTime().toLocalTime()
                    .format(DateTimeFormatter.ISO_LOCAL_TIME)
                ListItem(
                    index.toLong(),
                    "${message.sender} @ $time",
                    message.text,
                    icon = ResourceListIcon(
                        R.drawable.ic_info,
                        backgroundId = R.drawable.bubble,
                        backgroundTint = if (message.isMe) Color.BLUE else Color.GREEN,
                        tint = Color.WHITE,
                    )
                )
            })
            if (messages.isNotEmpty()) {
                binding.messages.scrollToPosition(messages.size - 1, true)
            }
        }

        binding.list.isVisible = !connected
        binding.messageInputLayout.isVisible = connected
    }

    private suspend fun sendText(text: String, to: IBluetoothDevice) = onIO {
        val message = "$TEXT_START$text$TEXT_END"
        to.write(message.toByteArray())
    }

    private fun receiveMessage(device: IBluetoothDevice): Message? {
        return tryOrDefault(null) {
            val response = device.readUntil(TEXT_END)
            val startMarker = TEXT_START
            val endMarker = TEXT_END.substring(0, TEXT_END.length - 1)

            if (!response.startsWith(startMarker) || !response.endsWith(endMarker)) {
                // Invalid message
                return null
            }

            val text = response
                .substringAfter(startMarker)
                .substringBefore(endMarker)
            Message(
                connectedDevice?.name ?: "??",
                text,
                Instant.now(),
                false
            )
        }
    }

    private fun IBluetoothDevice.readUntil(text: String): String {
        var matchIndex = 0
        return readUntil { char ->
            if (char == text[matchIndex]) {
                matchIndex++
                matchIndex >= text.length
            } else {
                matchIndex = 0
                false
            }
        }
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(layoutInflater, container, false)
    }

    data class Message(val sender: String, val text: String, val time: Instant, val isMe: Boolean)

    companion object {
        private const val TEXT_START = "--TEXTSTART--"
        private const val TEXT_END = "--TEXTEND--"
    }
}