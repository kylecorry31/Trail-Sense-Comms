package com.kylecorry.trail_sense_comms.infrastructure

import com.kylecorry.luna.streams.readUntil
import java.io.InputStream

fun InputStream.readUntil(text: String): String {
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