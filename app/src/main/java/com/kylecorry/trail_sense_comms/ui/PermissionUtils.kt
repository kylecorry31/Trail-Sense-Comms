package com.kylecorry.trail_sense_comms.ui

import android.Manifest
import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions

object PermissionUtils {

    fun hasBluetoothPermission(context: Context): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        return permissions.all { Permissions.hasPermission(context, it) }
    }

}