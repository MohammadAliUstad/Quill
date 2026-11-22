package com.yugentech.quill.reader.sound.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yugentech.quill.reader.sound.model.BackgroundSound
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Manages playback of looping background sounds. Sounds fade in as they start and fade out as
// they stop or give way to another; two players crossfade where the track loops, so it has no
// gap. Everything runs on the main thread, through [handler].
class BackgroundSoundService(private val context: Context) {

    private var activePlayer: ExoPlayer? = null
    private var nextPlayer: ExoPlayer? = null
    private var currentSound = BackgroundSound.NONE
    private var crossfadeAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null
    private var isLooping = false
    private var positionMonitor: Runnable? = null
    private var previewEnd: Runnable? = null
    private var crossfadeScheduled = false
    private var targetVolume = MAX_VOLUME
    private var isStopping = false

    // The start/stop fade, 0..1, on top of targetVolume (see applyVolumes)
    private var fadeGain = 1f

    // How far through the loop crossfade the players are, or null when they aren't crossfading
    private var crossfadeProgress: Float? = null

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val CROSSFADE_START_OFFSET = 2000L
        private const val CROSSFADE_DURATION = 2000L
        private const val PREVIEW_DURATION = 2000L
        private const val POSITION_CHECK_INTERVAL = 100L
        private const val MAX_VOLUME = 1.0f

        // A sound fading in as it starts, and out as it stops
        private const val FADE_IN_DURATION = 900L
        private const val FADE_OUT_DURATION = 700L

        // Shorter, when one sound gives way to another, so the new one isn't kept waiting
        private const val SWITCH_FADE_OUT_DURATION = 400L

        // The preview clip's own fades, in and out, within its two seconds
        private const val PREVIEW_FADE_DURATION = 200L
    }

    // Plays a short clip of the sound for user selection. It fades in, and fades out at the end
    // of the clip; a preview already playing fades out quickly first
    fun playPreview(sound: BackgroundSound, volume: Float = MAX_VOLUME) {
        handler.post {
            handler.removeCallbacksAndMessages(null)

            if (activePlayer != null) {
                fadeTo(0f, PREVIEW_FADE_DURATION) {
                    releaseInternal()
                    startPreview(sound, volume)
                }
            } else {
                releaseInternal()
                startPreview(sound, volume)
            }
        }
    }

    // Main entry point to start playing a specific sound
    fun play(sound: BackgroundSound, volume: Float = MAX_VOLUME) {
        handler.post {

            if (sound == BackgroundSound.NONE) {
                stopInternal()
                return@post
            }

            cancelPreviewEnd()

            // Already the sound playing (or fading out): keep it, and bring it back up if it was
            // on its way out
            if (isLooping && currentSound == sound) {
                targetVolume = volume
                if (isStopping) {
                    isStopping = false
                    // A stop on the main thread cleared the loop monitor; the sound keeps looping
                    // only while it runs
                    positionMonitor?.let {
                        handler.removeCallbacks(it)
                        handler.post(it)
                    }
                    fadeTo(1f, FADE_IN_DURATION)
                } else {
                    applyVolumes()
                }
                return@post
            }

            // Another sound (or a preview) playing: it fades out, then the new one fades in
            if (activePlayer != null) {
                isStopping = true
                fadeTo(0f, SWITCH_FADE_OUT_DURATION) {
                    releaseInternal()
                    startLooping(sound, volume)
                }
                return@post
            }

            releaseInternal()
            startLooping(sound, volume)
        }
    }

    // Fades the sound out, then releases the players. Called on the main thread (as the reader
    // closes) it starts fading right there, without waiting behind queued work; the service
    // outlives the reader, so the fade finishes after it's gone
    fun stop() {
        runNow { stopInternal() }
    }

    fun setVolume(volume: Float) {
        handler.post {
            targetVolume = volume
            applyVolumes()
        }
    }

    // Cleans up all players, animators, and handlers to prevent leaks. Immediate, no fade
    fun release() {
        runNow { releaseInternal() }
    }

    private fun runNow(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            handler.removeCallbacksAndMessages(null)
            block()
        } else {
            handler.post(block)
        }
    }

    private fun stopInternal() {
        cancelPreviewEnd()
        if (activePlayer == null) {
            releaseInternal()
            return
        }
        isStopping = true
        // Replaces any fade under way, including a switch's: it no longer starts its new sound
        fadeTo(0f, FADE_OUT_DURATION) { releaseInternal() }
    }

    private fun startPreview(sound: BackgroundSound, volume: Float) {
        if (sound == BackgroundSound.NONE) return

        sound.resId?.let { resId ->
            try {
                val uri = "android.resource://${context.packageName}/$resId"
                currentSound = sound
                isLooping = false
                isStopping = false
                targetVolume = volume
                fadeGain = 0f

                activePlayer = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    repeatMode = Player.REPEAT_MODE_OFF
                    prepare()
                    playWhenReady = true
                }
                applyVolumes()
                fadeTo(1f, PREVIEW_FADE_DURATION)

                // Starts fading out so it's silent right at the end of the clip
                val end = Runnable {
                    previewEnd = null
                    isStopping = true
                    fadeTo(0f, PREVIEW_FADE_DURATION) { releaseInternal() }
                }
                previewEnd = end
                handler.postDelayed(end, PREVIEW_DURATION - PREVIEW_FADE_DURATION)

            } catch (e: Exception) {
                Timber.e(e, "Preview failed")
                releaseInternal()
            }
        }
    }

    private fun startLooping(sound: BackgroundSound, volume: Float) {
        sound.resId?.let { resId ->
            try {
                val uri = "android.resource://${context.packageName}/$resId"
                currentSound = sound
                isLooping = true

                targetVolume = volume
                crossfadeScheduled = false
                isStopping = false
                fadeGain = 0f

                // Initialize two players for seamless looping via crossfading
                activePlayer = createPlayer(uri).apply {
                    playWhenReady = true
                }
                nextPlayer = createPlayer(uri)
                applyVolumes()
                fadeTo(1f, FADE_IN_DURATION)

                startPositionMonitoring(uri)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start background sound")
                releaseInternal()
            }
        }
    }

    // Sets both players' volumes from the target volume, the start/stop fade and, while the
    // track loops, the crossfade between them
    private fun applyVolumes() {
        try {
            // Squared: loudness follows the gain roughly that way, so a straight-line gain would
            // seem to jump at the quiet end of a fade and hardly move at the loud end
            val base = targetVolume * fadeGain * fadeGain
            val progress = crossfadeProgress
            if (progress == null) {
                activePlayer?.volume = base
            } else {
                // Equal-power crossfade: the overall loudness holds steady through it
                activePlayer?.volume = base * cos(progress * Math.PI / 2.0).toFloat()
                nextPlayer?.volume = base * sin(progress * Math.PI / 2.0).toFloat()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adjusting volume")
        }
    }

    // Eases fadeGain to [target] over [fullDuration] (scaled by how far it has to go, so a fade
    // reversed halfway takes half as long), then runs [onEnd]. Replaces any fade under way
    // without running that one's onEnd
    private fun fadeTo(target: Float, fullDuration: Long, onEnd: (() -> Unit)? = null) {
        cancelFade()

        val from = fadeGain
        val duration = (fullDuration * abs(target - from)).toLong()
        if (duration <= 0L) {
            fadeGain = target
            applyVolumes()
            onEnd?.invoke()
            return
        }

        fadeAnimator = ValueAnimator.ofFloat(from, target).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                fadeGain = animation.animatedValue as Float
                applyVolumes()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    fadeAnimator = null
                    fadeGain = target
                    applyVolumes()
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    private fun cancelFade() {
        fadeAnimator?.let {
            // Listeners off first: cancelling still calls onAnimationEnd
            it.removeAllListeners()
            it.removeAllUpdateListeners()
            it.cancel()
        }
        fadeAnimator = null
    }

    private fun cancelPreviewEnd() {
        previewEnd?.let { handler.removeCallbacks(it) }
        previewEnd = null
    }

    // Helper to build a simple ExoPlayer instance
    private fun createPlayer(uri: String) = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        prepare()
    }

    // Monitors playback progress to trigger crossfade near the end of the track
    private fun startPositionMonitoring(uri: String) {
        crossfadeScheduled = false

        positionMonitor = object : Runnable {
            override fun run() {
                if (!isLooping) return

                try {
                    val duration = activePlayer?.duration ?: 0
                    val position = activePlayer?.currentPosition ?: 0

                    if (!crossfadeScheduled && duration > 0 && position >= (duration - CROSSFADE_START_OFFSET)) {
                        crossfadeScheduled = true
                        performCrossfade(uri)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in position monitoring")
                }

                handler.postDelayed(this, POSITION_CHECK_INTERVAL)
            }
        }

        handler.post(positionMonitor!!)
    }

    // Seamlessly transitions from active player to next player using equal-power crossfade
    private fun performCrossfade(uri: String) {
        if (!isLooping) return

        crossfadeProgress = 0f
        nextPlayer?.apply {
            seekTo(0)
            playWhenReady = true
        }
        applyVolumes()

        crossfadeAnimator?.let {
            it.removeAllListeners()
            it.removeAllUpdateListeners()
            it.cancel()
        }
        crossfadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CROSSFADE_DURATION
            interpolator = LinearInterpolator()

            // applyVolumes works out each player's gain from the progress (and any fade)
            addUpdateListener { animation ->
                crossfadeProgress = animation.animatedValue as Float
                applyVolumes()
            }

            // Swap players when transition completes so the cycle continues
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isLooping) return

                    activePlayer?.apply {
                        stop()
                        seekTo(0)
                        prepare()
                    }

                    val temp = activePlayer
                    activePlayer = nextPlayer
                    nextPlayer = temp
                    crossfadeProgress = null
                    applyVolumes()

                    positionMonitor?.let { handler.removeCallbacks(it) }
                    startPositionMonitoring(uri)
                }
            })

            start()
        }
    }

    private fun releaseInternal() {
        try {
            positionMonitor?.let { handler.removeCallbacks(it) }
            positionMonitor = null
            cancelPreviewEnd()
            cancelFade()

            // Listeners off before cancelling: cancelling still calls onAnimationEnd, which would
            // swap the players and start monitoring again
            crossfadeAnimator?.removeAllListeners()
            crossfadeAnimator?.removeAllUpdateListeners()
            crossfadeAnimator?.cancel()

            crossfadeAnimator = null
            crossfadeProgress = null
            isLooping = false
            isStopping = false
            fadeGain = 1f

            activePlayer?.stop()
            activePlayer?.release()
            activePlayer = null

            nextPlayer?.stop()
            nextPlayer?.release()
            nextPlayer = null

            currentSound = BackgroundSound.NONE
            crossfadeScheduled = false
        } catch (e: Exception) {
            Timber.e(e, "Error releasing resources")
        }
    }
}
