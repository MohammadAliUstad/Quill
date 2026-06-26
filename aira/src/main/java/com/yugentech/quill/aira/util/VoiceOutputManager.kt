package com.yugentech.quill.aira.util

import android.media.MediaPlayer
import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class VoiceOutputManager(
    private val functions: FirebaseFunctions,
    private val cacheDir: File
) {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String) {
        stop()

        try {
            val payload = hashMapOf("text" to text)
            val result = functions
                .getHttpsCallable("speakText")
                .call(payload)
                .await()

            val audioBase64 = (result.getData() as? Map<*, *>)?.get("audio") as? String
                ?: throw Exception("No audio data received")

            val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
            val tempFile = File.createTempFile("aira_speech", ".mp3", cacheDir)
            
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "TTS failed")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}
