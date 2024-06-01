package live.mehiz.mpvkt.ui.player

import android.media.AudioManager
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.preferences.PlayerPreferences
import org.koin.java.KoinJavaComponent.inject

class PlayerViewModel(
  val activity: PlayerActivity,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)

  private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
  val subtitleTracks = _subtitleTracks.asStateFlow()
  private val _selectedSubtitles = MutableStateFlow((-1))
  val selectedSubtitles = _selectedSubtitles.asStateFlow()

  private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
  val audioTracks = _audioTracks.asStateFlow()
  private val _selectedAudio = MutableStateFlow(-1)
  val selectedAudio = _selectedAudio.asStateFlow()

  private val _pos = MutableStateFlow(0f)
  val pos = _pos.asStateFlow()

  var duration: Float = 0f

  private val _readAhead = MutableStateFlow(0f)
  val readAhead = _readAhead.asStateFlow()

  private val _paused = MutableStateFlow(false)
  val paused = _paused.asStateFlow()

  private val _controlsShown = MutableStateFlow(true)
  val controlsShown = _controlsShown.asStateFlow()
  private val _seekBarShown = MutableStateFlow(true)
  val seekBarShown = _seekBarShown.asStateFlow()
  private val _areControlsLocked = MutableStateFlow(false)
  val areControlsLocked = _areControlsLocked.asStateFlow()

  val getTrackLanguage: (Int) -> String = {
    if (it != -1) MPVLib.getPropertyString("track-list/$it/lang") ?: ""
    else "Off"
  }
  val getTrackTitle: (Int) -> String = {
    if (it != -1) MPVLib.getPropertyString("track-list/$it/title") ?: ""
    else "Off"
  }
  val getTrackMPVId: (Int) -> Int = {
    if (it != -1) MPVLib.getPropertyInt("track-list/$it/id")
    else -1
  }
  val getTrackType: (Int) -> String? = {
    MPVLib.getPropertyString("track-list/$it/type")
  }

  fun loadTracks() {
    val tracksCount = MPVLib.getPropertyInt("track-list/count")!!
    val possibleTrackTypes = listOf("video", "audio", "sub")
    val vidTracks = mutableListOf<Track>()
    val subTracks = mutableListOf<Track>()
    val audioTracks = mutableListOf<Track>()
    for (i in 0..<tracksCount) {
      val type = getTrackType(i) ?: continue
      if (!possibleTrackTypes.contains(type)) continue
      when (type) {
        "audio" -> audioTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
        "video" -> vidTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
        "sub" -> subTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
        else -> throw IllegalStateException()
      }
    }
    _subtitleTracks.update { subTracks }
    _selectedSubtitles.update { activity.player.sid }
    _audioTracks.update { audioTracks }
    _selectedAudio.update { activity.player.aid }
  }

  fun selectSub(id: Int) {
    activity.player.sid = id
    _selectedSubtitles.update { id }
  }

  fun selectAudio(id: Int) {
    activity.player.aid = id
    _selectedAudio.update { id }
  }

  fun updatePlayBackPos(pos: Float) {
    _pos.value = pos
  }

  fun updateReadAhead(value: Long) {
    _readAhead.value = pos.value + value.toFloat()
  }

  fun pauseUnpause() {
    if (paused.value) unpause()
    else pause()
  }

  fun pause() {
    activity.player.paused = true
    _paused.value = true
  }

  fun unpause() {
    activity.player.paused = false
    _paused.value = false
  }

  fun showControls() {
    _controlsShown.value = true
  }

  fun hideControls() {
    _controlsShown.value = false
    activity.windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
  }

  fun toggleControls() {
    if (controlsShown.value) hideControls()
    if (seekBarShown.value) hideSeekBar()
    else showControls()
  }

  fun toggleSeekBar() {
    _seekBarShown.value = !seekBarShown.value
  }

  fun hideSeekBar() {
    _seekBarShown.value = false
  }

  fun showSeekBar() {
    _seekBarShown.value = true
  }

  fun lockControls() {
    _areControlsLocked.value = true
  }

  fun unlockControls() {
    _areControlsLocked.value = false
  }

  fun seekBy(offset: Int) {
    activity.player.timePos = activity.player.timePos?.plus(offset)
  }

  fun seekTo(position: Int) {
    if (position < 0) return
    if (position > (activity.player.duration ?: 0)) return
    activity.player.timePos = position
  }

  fun changeBrightnessWithDrag(
    dragAmount: Float,
  ) {
    activity.window.attributes = activity.window.attributes.apply {
      screenBrightness = dragAmount.coerceIn(0f, 1f)
    }
  }

  fun changeVolumeWithDrag(dragAmount: Float) {
    activity.audioManager.setStreamVolume(
      AudioManager.STREAM_MUSIC,
      dragAmount.toInt(),
      AudioManager.FLAG_SHOW_UI,
    )
  }

  fun changeVideoAspect(aspect: VideoAspect) {
    var ratio = -1.0
    var pan = 1.0
    when (aspect) {
      VideoAspect.Crop -> {
        pan = 1.0
        playerPreferences.videoAspect.set(VideoAspect.Crop)
      }

      VideoAspect.Fit -> {
        pan = 0.0
        MPVLib.setPropertyDouble("panscan", 0.0)
      }

      VideoAspect.Stretch -> {
        ratio =
          activity.resources.displayMetrics.widthPixels / activity.resources.displayMetrics.heightPixels.toDouble()
        pan = 0.0
      }
    }
    MPVLib.setPropertyDouble("panscan", pan)
    MPVLib.setPropertyDouble("video-aspect-override", ratio)
    playerPreferences.videoAspect.set(aspect)
  }
}

data class Track(
  val id: Int,
  val name: String,
  val language: String?,
)
