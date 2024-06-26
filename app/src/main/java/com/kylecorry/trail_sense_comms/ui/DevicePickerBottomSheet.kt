package com.kylecorry.trail_sense_comms.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.connection.bluetooth.BluetoothScanner
import com.kylecorry.andromeda.connection.bluetooth.BluetoothService
import com.kylecorry.andromeda.connection.bluetooth.IBluetoothDevice
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.interval
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.trail_sense_comms.databinding.DevicePickerSheetBinding
import com.kylecorry.trail_sense_comms.infrastructure.nearby.NearbyDevice
import com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth.BluetoothNearbyDevice

class DevicePickerBottomSheet : BoundBottomSheetDialogFragment<DevicePickerSheetBinding>() {

    private val hooks = Hooks(stateThrottleMs = AndromedaFragment.INTERVAL_60_FPS) {
        onUpdate()
    }

    private var devices by hooks.state(emptyList<BluetoothDevice>())
    private val bluetooth by lazy { BluetoothService(requireContext()) }
    private val scanner by lazy { BluetoothScanner(requireContext()) }

    private var onDeviceSelected: (NearbyDevice) -> Unit = {}

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!PermissionUtils.hasBluetoothPermission(requireContext())) {
            return
        }

        interval(100) {
            devices = bluetooth.bondedDevices
            // TODO: Only show devices that are broadcasting the UUID of TS Comms
//                .filter {
//                it.uuids?.any { it.uuid == ConnectionDetails.bluetoothUUID } == true
//            }
        }
        // TODO: Use scanner
//        observe(scanner) {
//            devices = (scanner.devices + bluetooth.bondedDevices).distinctBy { it.address }
//        }
        isCancelable = false
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): DevicePickerSheetBinding {
        return DevicePickerSheetBinding.inflate(layoutInflater, container, false)
    }

    @SuppressLint("MissingPermission")
    private fun onUpdate() {
        hooks.effect("devices", devices) {
            binding.list.setItems(devices.filter { it.name != null }.mapIndexed { index, device ->
                ListItem(index.toLong(), device.name ?: "??", device.address) {
                    onDeviceSelected(
                        BluetoothNearbyDevice(
                            bluetooth.getSecureDevice(
                                device.address,
                                ConnectionDetails.bluetoothUUID
                            )
                        )
                    )
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        hooks.startStateUpdates()
    }

    override fun onPause() {
        super.onPause()
        hooks.stopStateUpdates()
    }

    fun setOnDeviceSelected(onDeviceSelected: (NearbyDevice) -> Unit) {
        this.onDeviceSelected = onDeviceSelected
    }
}