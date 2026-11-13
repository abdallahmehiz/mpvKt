package live.mehiz.mpvkt.ui.player.manager

import live.mehiz.mpvkt.ui.player.mapState
import android.content.Context
import android.media.AudioManager
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import live.mehiz.mpvkt.preferences.AudioChannels
import live.mehiz.mpvkt.preferences.AudioPreferences
import kotlin.math.roundToInt

class AudioSettings(
  private val mpv: MPV,
  context: Context,
  private val preferences: AudioPreferences,
  scope: CoroutineScope,
) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  val delayMs: StateFlow<Int> = mpv.propFlow<Double>("audio-delay")
    .mapState(scope, preferences.defaultAudioDelay::get) { (it * 1000).roundToInt() }

  val channels: StateFlow<AudioChannels> = preferences.audioChannels.stateIn(scope)

  val pitchCorrection: StateFlow<Boolean> = combine(
    preferences.audioPitchCorrection.changes(),
    mpv.propFlow<Boolean>("audio-pitch-correction"),
  ) { preferred, current -> preferred || current == true }
    .stateIn(scope, SharingStarted.Eagerly, preferences.audioPitchCorrection.get())

  val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
  private val _streamVolume = MutableStateFlow(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
  val streamVolume = _streamVolume.asStateFlow()
  val mpvVolume: StateFlow<Int> = mpv.propFlow("volume", 100)
  private val boostCap: Int get() = (mpv.getPropertyInt("volume-max") ?: (preferences.volumeBoostCap.get() + 100)) - 100

  fun changeVolumeBy(change: Int) {
    if (boostCap > 0 && streamVolume.value == maxVolume) {
      if (mpvVolume.value == 100 && change < 0) setStreamVolume(streamVolume.value + change)
      val boosted = (mpvVolume.value + change).coerceAtLeast(100)
      if (boosted in 100..boostCap + 100) {
        setMpvVolume(boosted)
        return
      }
    }
    setStreamVolume(streamVolume.value + change)
  }

  fun setStreamVolume(volume: Int) {
    val newVolume = volume.coerceIn(0..maxVolume)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    _streamVolume.value = newVolume
  }

  fun setMpvVolume(volume: Int) {
    mpv.prop("volume", volume)
  }

  fun refreshStreamVolume() {
    _streamVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).also {
      if (it < maxVolume) setMpvVolume(100)
    }
  }

  fun setDelay(ms: Int) {
    mpv.prop("audio-delay", ms / 1000.0)
  }

  fun saveDelayAsDefault() {
    preferences.defaultAudioDelay.set(delayMs.value)
  }

  fun resetDelay() {
    setDelay(preferences.defaultAudioDelay.get())
  }

  fun setChannels(channels: AudioChannels) {
    preferences.audioChannels.set(channels)
    // the two use different properties, clear the other one
    if (channels == AudioChannels.ReverseStereo) {
      mpv.prop(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
    } else {
      mpv.prop(AudioChannels.ReverseStereo.property, "")
    }
    mpv.prop(channels.property, channels.value)
  }

  fun setPitchCorrection(enabled: Boolean) {
    preferences.audioPitchCorrection.set(enabled)
    mpv.prop("audio-pitch-correction", enabled)
  }
}
