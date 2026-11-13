package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import android.widget.Toast
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.ui.player.ChapterNode
import live.mehiz.mpvkt.ui.player.mapState
import live.mehiz.mpvkt.ui.player.toObject
import kotlin.time.Duration.Companion.milliseconds

class PlaybackState(
  private val mpv: MPV,
  private val context: Context,
  private val preferences: PlayerPreferences,
  private val json: Json,
  private val scope: CoroutineScope,
) {
  val paused: StateFlow<Boolean> = mpv.propFlow("pause", true)
  val pausedForCache: StateFlow<Boolean> = mpv.propFlow("paused-for-cache", false)
  val position: StateFlow<Int> = mpv.propFlow("time-pos", 0)
  val duration: StateFlow<Int> = mpv.propFlow("duration", 0)
  val speed: StateFlow<Float> = mpv.propFlow("speed", preferences.defaultSpeed.get())
  val readAhead: StateFlow<Float> = mpv.propFlow("demuxer-cache-duration", 0f)
  val mediaTitle: StateFlow<String> = mpv.propFlow("media-title", "")
  val chapter: StateFlow<Int> = mpv.propFlow("chapter", 0)
  val chapters: StateFlow<ImmutableList<Segment>> = mpv.propFlow<MPVNode>("chapter-list")
    .mapState(scope, { persistentListOf() }) { node ->
      node.toObject<List<ChapterNode>>(json).map { it.toSegment() }.toImmutableList()
    }

  fun setPaused(paused: Boolean) {
    mpv.prop("pause", paused)
  }

  fun togglePause() {
    mpv.command("cycle", "pause")
  }

  fun seekBy(offset: Int, precise: Boolean = false) {
    mpv.command("seek", offset.toString(), if (precise) "relative+exact" else "relative")
  }

  fun seekTo(position: Int, precise: Boolean = true) {
    if (position !in 0..duration.value) return
    mpv.command("seek", position.toString(), if (precise) "absolute" else "absolute+keyframes")
  }

  fun seekToChapter(index: Int) {
    mpv.prop("chapter", index)
    setPaused(false)
  }

  fun setSpeed(speed: Float) {
    mpv.prop("speed", speed)
  }

  fun setDefaultSpeed(speed: Float) {
    preferences.defaultSpeed.set(speed)
  }

  fun resetSpeed() {
    setSpeed(preferences.defaultSpeed.deleteAndGet())
  }

  private var speedBeforeHold: Float? = null

  fun holdSpeed(heldSpeed: Float) {
    if (speedBeforeHold != null || paused.value) return
    speedBeforeHold = speed.value
    setSpeed(heldSpeed)
  }

  fun releaseHeldSpeed() {
    setSpeed(speedBeforeHold ?: return)
    speedBeforeHold = null
  }

  private var timerJob: Job? = null
  private val _sleepTimerRemaining = MutableStateFlow(0)
  val sleepTimerRemaining = _sleepTimerRemaining.asStateFlow()

  fun startSleepTimer(seconds: Int) {
    timerJob?.cancel()
    _sleepTimerRemaining.value = seconds
    if (seconds < 1) return
    timerJob = scope.launch {
      for (time in seconds downTo 0) {
        _sleepTimerRemaining.value = time
        delay(1000.milliseconds)
      }
      setPaused(true)
      Toast.makeText(context, context.getString(R.string.toast_sleep_timer_ended), Toast.LENGTH_SHORT).show()
    }
  }
}
