package com.pulselink.beacon.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(output: File) {
        if (recorder != null) {
            stop()
        }
        outputFile = output

        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(output.absolutePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle error
                recorder = null
            }
        }
    }

    fun stop() {
        recorder?.let {
            try {
                it.stop()
            } catch (e: RuntimeException) {
                // RuntimeException is thrown if stop() is called immediately after start()
                // The file might be empty, so we should probably delete it.
                outputFile?.delete()
            }
            it.release()
        }
        recorder = null
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.also { recorder = it }
    }
}
