package com.kylecorry.trail_sense_comms.ui

import android.Manifest
import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions

object PermissionUtils {
    
    fun hasBluetoothPermission(context: Context): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

        return permissions.all { Permissions.hasPermission(context, it) }
    }

}