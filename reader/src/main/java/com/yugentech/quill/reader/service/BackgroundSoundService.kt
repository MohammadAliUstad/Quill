package com.yugentech.quill.reader.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearEasing
import android.view.animation.LinearInterpolator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yugentech.quill.reader.model.BackgroundSound
import timber.log.Timber
import kotlin.math.cos
import kotlin.math.sin

// Manages playback of looping background sounds using two players for gapless crossfading
class BackgroundSoundService(private val context: Context) {

    private var activePlayer: ExoPlayer? = null
    private var nextPlayer: ExoPlayer? = null
    private var currentSound = BackgroundSound.NONE
    private var volumeAnimator: ValueAnimator? = null
    private var crossfadeAnimator: ValueAnimator? = null
    private var isLooping = false
    private var positionMonitor: Runnable? = null
    private var crossfadeScheduled = false
    private var targetVolume = MAX_VOLUME
    private var isStopping = false

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val FADE_DURATION = 1500L
        private const val CROSSFADE_START_OFFSET = 2000L
        private const val CROSSFADE_DURATION = 2000L
        private const val POSITION_CHECK_INTERVAL = 100L
        private const val MAX_VOLUME = 1.0f
    }

    // Main entry point to start playing a specific sound
    fun play(sound: BackgroundSound, volume: Float = MAX_VOLUME) {
        handler.post {
            Timber.d("play() called with sound: ${sound.id}, volume: $volume")

            if (sound == BackgroundSound.NONE) {
                stop()
                return@post
            }

            if (isStopping) {
                Timber.d("Cancelling ongoing stop operation")
                isStopping = false
            }

            if (isLooping && currentSound == sound) {
                Timber.d("Same sound already playing, adjusting volume")
                fadeVolumeInternal(targetVolume, volume)
                return@post
            }

            Timber.d("Starting new sound: ${sound.id}")
            releaseInternal()

            sound.resId?.let { resId ->
                try {
                    val uri = "android.resource://${context.packageName}/$resId"
                    currentSound = sound
                    isLooping = true

                    targetVolume = volume
                    crossfadeScheduled = false
                    isStopping = false

                    // Initialize two players for seamless looping via crossfading
                    activePlayer = createPlayer(uri).apply {
                        this.volume = 0f
                        playWhenReady = true
                    }
                    nextPlayer = createPlayer(uri)

                    startPositionMonitoring(uri)

                    fadeVolumeInternal(0f, volume)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start background sound")
                    releaseInternal()
                }
            }
        }
    }

    // Gradually fades out audio and releases resources
    fun stop() {
        handler.post {
            Timber.d("stop() called")

            if (activePlayer?.isPlaying == true) {
                isStopping = true
                fadeVolumeInternal(targetVolume, 0f) {
                    if (isStopping) {
                        Timber.d("Fade complete, releasing player")
                        releaseInternal()
                    } else {
                        Timber.d("Fade complete, but stop was cancelled - keeping player")
                    }
                }
            } else {
                releaseInternal()
            }
        }
    }

    fun setVolume(volume: Float) {
        handler.post {
            fadeVolumeInternal(targetVolume, volume)
        }
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

        Timber.d("Starting crossfade")

        nextPlayer?.apply {
            volume = 0f
            seekTo(0)
            playWhenReady = true
        }

        crossfadeAnimator?.cancel()
        crossfadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CROSSFADE_DURATION
            interpolator = LinearInterpolator()

            // Calculate equal-power gain for smooth audio transition
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                try {
                    val fadeOutGain = cos(progress * Math.PI / 2.0).toFloat()
                    val fadeInGain = sin(progress * Math.PI / 2.0).toFloat()

                    activePlayer?.volume = targetVolume * fadeOutGain
                    nextPlayer?.volume = targetVolume * fadeInGain
                } catch (e: Exception) {
                    Timber.e(e, "Error during crossfade")
                }
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

                    positionMonitor?.let { handler.removeCallbacks(it) }
                    startPositionMonitoring(uri)
                }
            })

            start()
        }
    }

    private fun fadeVolumeInternal(
        from: Float,
        to: Float,
        duration: Long = FADE_DURATION,
        onEnd: (() -> Unit)? = null
    ) {
        volumeAnimator?.cancel()
        volumeAnimator?.removeAllListeners()
        volumeAnimator?.removeAllUpdateListeners()

        volumeAnimator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val newVolume = animation.animatedValue as Float
                targetVolume = newVolume
                try {
                    activePlayer?.volume = newVolume
                } catch (e: Exception) {
                    Timber.e(e, "Error adjusting volume")
                }
            }

            onEnd?.let {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = it()
                })
            }

            start()
        }
    }

    // Cleans up all players, animators, and handlers to prevent leaks
    fun release() {
        handler.post { releaseInternal() }
    }

    private fun releaseInternal() {
        Timber.d("releaseInternal() called")
        try {
            positionMonitor?.let { handler.removeCallbacks(it) }

            volumeAnimator?.cancel()
            volumeAnimator?.removeAllListeners()
            volumeAnimator?.removeAllUpdateListeners()

            crossfadeAnimator?.cancel()
            crossfadeAnimator?.removeAllListeners()
            crossfadeAnimator?.removeAllUpdateListeners()

            volumeAnimator = null
            crossfadeAnimator = null
            isLooping = false
            isStopping = false

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