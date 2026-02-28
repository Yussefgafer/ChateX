package com.kai.ghostmesh.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

class AudioManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null

    fun startRecording(): File? {
        return try {
            audioFile = File(context.cacheDir, "spectral_voice_${System.currentTimeMillis()}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            audioFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun playAudio(base64: String, onComplete: () -> Unit = {}) {
        try {
            stopPlayback()
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "play_temp_${System.currentTimeMillis()}.m4a")
            FileOutputStream(tempFile).use { it.write(bytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                    tempFile.delete()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
        stopPlayback()
    }
}
