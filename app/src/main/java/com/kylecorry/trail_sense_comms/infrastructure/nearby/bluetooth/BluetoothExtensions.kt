package com.kylecorry.trail_sense_comms.infrastructure.nearby.bluetooth

import com.kylecorry.andromeda.connection.bluetooth.IBluetoothDevice

fun IBluetoothDevice.readUntil(text: String): String {
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