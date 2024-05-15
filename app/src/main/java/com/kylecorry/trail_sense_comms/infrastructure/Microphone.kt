package com.kylecorry.trail_sense_comms.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import com.kylecorry.andromeda.permissions.Permissions


class Microphone(
    private val context: Context,
    private val sampleRate: Int,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

) {
    private var recorder: AudioRecord? = null

    fun read(byteArray: ByteArray, offset: Int, size: Int): Int {
        return recorder?.read(byteArray, offset, size) ?: 0
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!Permissions.canRecordAudio(context)) {
            return
        }

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        recorder?.startRecording()

        recorder?.audioSessionId?.let {
            if (AcousticEchoCanceler.isAvailable()) {
                val echo = AcousticEchoCanceler.create(it)
                echo.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                val noise = NoiseSuppressor.create(it)
                noise.enabled = true
            }
            if (AutomaticGainControl.isAvailable()) {
                val gain = AutomaticGainControl.create(it)
                gain.enabled = true
            }
        }

    }

    fun stop() {
        recorder?.stop()
        recorder = null
    }
}