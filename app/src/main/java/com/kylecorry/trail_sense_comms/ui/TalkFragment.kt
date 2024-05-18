package com.kylecorry.trail_sense_comms.ui

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.loading.AlertLoadingIndicator
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense_comms.R
import com.kylecorry.trail_sense_comms.databinding.FragmentTalkBinding
import com.kylecorry.trail_sense_comms.infrastructure.Microphone
import com.kylecorry.trail_sense_comms.infrastructure.nearby.DeviceManager
import com.kylecorry.trail_sense_comms.infrastructure.nearby.NearbyDevice
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TalkFragment : BoundFragment<FragmentTalkBinding>() {

    // TODO: Try out just in time connection (don't connect until message is sent)
    // TODO: Run in background
    // TODO: Show connection status of all devices in the group
    // TODO: Handle permissions

    private val manager by lazy { DeviceManager(requireContext()) }
    private var connectedDevice by state<NearbyDevice?>(null)
    private var connected by state(false)
    private var isConnecting by state(false)
    private var isTransmitting by state(false)
    private var sheet: DevicePickerBottomSheet? = null
    private var connectingDialog: ILoadingIndicator? = null
    private val minBufferSize by lazy {
        AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }
    private val receiveBuffer by lazy { ByteArray(minBufferSize) }
    private val transmitBuffer by lazy { ByteArray(minBufferSize) }
    private val audioTrack by lazy {
        AudioTrack.Builder()
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .build()
    }
    private val microphone by lazy {
        Microphone(
            requireContext(),
            16000,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_IN_MONO,
            minBufferSize
        )
    }

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

        binding.micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTransmitting = true
                }

                MotionEvent.ACTION_UP -> {
                    isTransmitting = false
                }
            }
            true
        }

        // Receive
        inBackground {
            onIO {
                while (true) {
                    // TODO: Handle disconnect
                    val device = connectedDevice ?: continue
                    receiveAudio(device)
                }
            }
        }

        // Transmit
        inBackground {
            onIO {
                while (true) {
                    if (!isTransmitting) {
                        continue
                    }

                    val device = connectedDevice ?: continue
                    sendAudio(device)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manager.start()
        audioTrack.play()
        requestPermissions(listOf(Manifest.permission.RECORD_AUDIO)) {
            if (Permissions.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
                microphone.start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        manager.stop()
        microphone.stop()
        audioTrack.stop()
    }

    @SuppressLint("MissingPermission")
    override fun onUpdate() {
        super.onUpdate()
        effect("connection", connectedDevice?.name, connected) {
            binding.toolbar.subtitle.text =
                if (connected) "Connected to ${connectedDevice?.name}" else "Disconnected"
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

        effect("transmitting", isTransmitting) {
            Colors.setImageColor(
                binding.micButton,
                if (isTransmitting) Resources.getAndroidColorAttr(
                    requireContext(),
                    android.R.attr.colorPrimary
                ) else Resources.getAndroidColorAttr(
                    requireContext(),
                    android.R.attr.colorBackgroundFloating
                )
            )
            audioTrack.setVolume(if (isTransmitting) 0f else 1f)
        }
    }

    private suspend fun sendAudio(to: NearbyDevice) = onIO {
        microphone.read(transmitBuffer, 0, transmitBuffer.size)
        to.outputStream?.write(transmitBuffer)
    }

    private fun receiveAudio(device: NearbyDevice) {
        tryOrDefault(null) {
            val readBytes = device.inputStream?.read(receiveBuffer, 0, receiveBuffer.size) ?: return
            if (!isTransmitting && readBytes > 0) {
                audioTrack.write(receiveBuffer, 0, readBytes)
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentTalkBinding {
        return FragmentTalkBinding.inflate(layoutInflater, container, false)
    }
}