package com.yugentech.quill.reader.sound.repository

import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.quill.reader.sound.service.BackgroundSoundService

class BackgroundSoundRepositoryImpl(
    private val soundService: BackgroundSoundService
) : BackgroundSoundRepository {
    override fun play(sound: BackgroundSound, volume: Float) = soundService.play(sound, volume)
    override fun stop() = soundService.stop()
    override fun setVolume(volume: Float) = soundService.setVolume(volume)
    override fun release() = soundService.release()
}
