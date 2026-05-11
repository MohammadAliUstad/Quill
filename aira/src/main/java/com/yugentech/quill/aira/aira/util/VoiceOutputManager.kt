package com.yugentech.quill.aira.aira.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class VoiceOutputManager(
    private val context: Context,
    private val functions: FirebaseFunctions
) {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String) {
        try {
            val result = functions.getHttpsCallable("airaTts")
                .call(mapOf("text" to text, "accentCode" to "en-US"))
                .await()

            val data = result.data as? Map<*, *>
            val base64Audio = data?.get("audio") as? String

            if (base64Audio == null) {
                Timber.e("TTS: No audio data returned")
                return
            }

            withContext(Dispatchers.IO) {
                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                val tempFile = File.createTempFile("aira_speech", ".mp3", context.cacheDir)
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                playAudio(tempFile.absolutePath)
            }

        } catch (e: Exception) {
            Timber.e(e, "TTS Firebase Call Failed")
        }
    }

    private fun playAudio(filePath: String) {
        stop()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(filePath)
            prepare()
            start()
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
                File(filePath).delete()
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun destroy() {
        stop()
    }
}