package com.kylecorry.trail_sense_comms.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.loading.AlertLoadingIndicator
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.speech.TextToSpeech
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense_comms.R
import com.kylecorry.trail_sense_comms.databinding.FragmentMainBinding
import com.kylecorry.trail_sense_comms.infrastructure.nearby.DeviceManager
import com.kylecorry.trail_sense_comms.infrastructure.nearby.NearbyDevice
import com.kylecorry.trail_sense_comms.infrastructure.readUntil
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainFragment : BoundFragment<FragmentMainBinding>() {

    // TODO: Try out just in time connection (don't connect until message is sent)
    // TODO: Run in background
    // TODO: Show connection status of all devices in the group
    // TODO: Handle permissions

    private val manager by lazy { DeviceManager(requireContext()) }
    private var shouldReadMessages by state(false)
    private var connectedDevice by state<NearbyDevice?>(null)
    private var connected by state(false)
    private var isConnecting by state(false)
    private var messages by state(emptyList<Message>())
    private var sheet: DevicePickerBottomSheet? = null
    private var connectingDialog: ILoadingIndicator? = null
    private val tts by lazy { TextToSpeech(requireContext()) }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!PermissionUtils.hasBluetoothPermission(requireContext())) {
            return
        }

        manager.onConnectionChanged {
            connected = manager.connected
            isConnecting = manager.isConnecting
            connectedDevice = manager.connectedDevice
        }

        binding.readOutMessagesSwitch.setOnCheckedChangeListener { _, isChecked ->
            shouldReadMessages = isChecked
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
        manager.start()
    }

    override fun onPause() {
        super.onPause()
        manager.stop()
        tts.cancel()
    }

    @SuppressLint("MissingPermission")
    override fun onUpdate() {
        super.onUpdate()
        effect("connection", connectedDevice?.name, connected) {
            binding.toolbar.subtitle.text =
                if (connected) "Connected to ${connectedDevice?.name}" else "Disconnected"
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

            val last = messages.lastOrNull()
            if (last != null && !last.isMe && shouldReadMessages) {
                tts.speak(last.text)
            }
        }

        effect("device_selector", connected) {
            if (!connected) {
                sheet?.dismiss()
                sheet = DevicePickerBottomSheet()
                sheet?.setOnDeviceSelected {
                    sheet?.dismiss()
                    manager.connect(it)
                }
                sheet?.show(this)
            } else {
                sheet?.dismiss()
                sheet = null
            }
        }

        effect("connecting", isConnecting) {
            if (isConnecting) {
                connectingDialog = AlertLoadingIndicator(
                    requireContext(),
                    getString(R.string.connecting)
                )
                connectingDialog?.show()
            } else {
                connectingDialog?.hide()
                connectingDialog = null
            }
        }

        binding.readOutMessagesSwitch.isChecked = shouldReadMessages

        binding.messageInputLayout.isVisible = connected
    }

    private suspend fun sendText(text: String, to: NearbyDevice) = onIO {
        val message = "$TEXT_START$text$TEXT_END"
        // TODO: Read into a buffer
        to.outputStream?.write(message.toByteArray())
    }

    private fun receiveMessage(device: NearbyDevice): Message? {
        return tryOrDefault(null) {
            val response = device.inputStream?.readUntil(TEXT_END) ?: return null
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