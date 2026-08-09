package com.yugentech.quill.reader.sound.repository

import com.yugentech.quill.reader.sound.model.BackgroundSound

interface BackgroundSoundRepository {
    fun play(sound: BackgroundSound, volume: Float)
    fun stop()
    fun setVolume(volume: Float)
    fun release()
}
