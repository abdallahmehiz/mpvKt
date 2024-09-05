package live.mehiz.mpvkt.ui.player

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import org.koin.java.KoinJavaComponent.inject

@Suppress("TooManyFunctions")
class PlayerViewModel(
  private val activity: PlayerActivity,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)

  private val _currentDecoder = MutableStateFlow(getDecoderFromValue(MPVLib.getPropertyString("hwdec")))
  val currentDecoder = _currentDecoder.asStateFlow()

  val mediaTitle = MutableStateFlow("")

  val isLoading = MutableStateFlow(true)
  val playbackSpeed = MutableStateFlow(playerPreferences.defaultSpeed.get())

  private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
  val subtitleTracks = _subtitleTracks.asStateFlow()
  private val _selectedSubtitles = MutableStateFlow(Pair(-1, -1))
  val selectedSubtitles = _selectedSubtitles.asStateFlow()

  private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
  val audioTracks = _audioTracks.asStateFlow()
  private val _selectedAudio = MutableStateFlow(-1)
  val selectedAudio = _selectedAudio.asStateFlow()

  var chapters: List<Segment> = listOf()
  private val _currentChapter = MutableStateFlow<Segment?>(null)
  val currentChapter = _currentChapter.asStateFlow()

  private val _pos = MutableStateFlow(0f)
  val pos = _pos.asStateFlow()

  val duration = MutableStateFlow(0f)

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

  val playerUpdate = MutableStateFlow(PlayerUpdates.None)
  val isBrightnessSliderShown = MutableStateFlow(false)
  val isVolumeSliderShown = MutableStateFlow(false)
  val currentBrightness = MutableStateFlow(
    Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
      .normalize(0f, 255f, 0f, 1f),
  )
  val currentVolume = MutableStateFlow(activity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

  val sheetShown = MutableStateFlow(Sheets.None)
  val panelShown = MutableStateFlow(Panels.None)

  // Pair(startingPosition, seekAmount)
  val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)

  private var timerJob: Job? = null
  private val _remainingTime = MutableStateFlow(0)
  val remainingTime = _remainingTime.asStateFlow()

  fun startTimer(seconds: Int) {
    timerJob?.cancel()
    _remainingTime.value = seconds
    if (seconds < 1) return
    timerJob = viewModelScope.launch {
      for (time in seconds downTo 0) {
        _remainingTime.value = time
        delay(1000)
      }
      pause()
      Toast.makeText(activity, activity.getString(R.string.toast_sleep_timer_ended), Toast.LENGTH_SHORT).show()
    }
  }

  fun getDecoder() {
    _currentDecoder.update { getDecoderFromValue(activity.player.hwdecActive) }
  }

  fun cycleDecoders() {
    MPVLib.setPropertyString(
      "hwdec",
      when (currentDecoder.value) {
        Decoder.HWPlus -> Decoder.HW.value
        Decoder.HW -> Decoder.SW.value
        Decoder.SW -> Decoder.HWPlus.value
        Decoder.AutoCopy -> Decoder.SW.value
        Decoder.Auto -> Decoder.SW.value
      },
    )
    val newDecoder = activity.player.hwdecActive
    if (newDecoder != currentDecoder.value.value) {
      _currentDecoder.update { getDecoderFromValue(newDecoder) }
    }
  }

  fun updateDecoder(decoder: Decoder) {
    MPVLib.setPropertyString("hwdec", decoder.value)
    val newDecoder = activity.player.hwdecActive
    if (newDecoder != currentDecoder.value.value) {
      _currentDecoder.update { getDecoderFromValue(newDecoder) }
    }
  }

  val getTrackLanguage: (Int) -> String = {
    if (it != -1) {
      MPVLib.getPropertyString("track-list/$it/lang") ?: ""
    } else {
      activity.getString(R.string.player_sheets_tracks_off)
    }
  }
  val getTrackTitle: (Int) -> String = {
    if (it != -1) {
      MPVLib.getPropertyString("track-list/$it/title") ?: ""
    } else {
      activity.getString(R.string.player_sheets_tracks_off)
    }
  }
  val getTrackMPVId: (Int) -> Int = {
    if (it != -1) {
      MPVLib.getPropertyInt("track-list/$it/id")
    } else {
      -1
    }
  }
  val getTrackType: (Int) -> String? = {
    MPVLib.getPropertyString("track-list/$it/type")
  }

  private var trackLoadingJob: Job? = null
  fun loadTracks() {
    trackLoadingJob?.cancel()
    trackLoadingJob = viewModelScope.launch {
      val possibleTrackTypes = listOf("video", "audio", "sub")
      val vidTracks = mutableListOf<Track>()
      val subTracks = mutableListOf<Track>()
      val audioTracks = mutableListOf(Track(-1, activity.getString(R.string.player_sheets_tracks_off), null))
      try {
        val tracksCount = MPVLib.getPropertyInt("track-list/count") ?: 0
        for (i in 0..<tracksCount) {
          val type = getTrackType(i)
          if (!possibleTrackTypes.contains(type) || type == null) continue
          when (type) {
            "sub" -> subTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
            "audio" -> audioTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
            "video" -> vidTracks.add(Track(getTrackMPVId(i), getTrackTitle(i), getTrackLanguage(i)))
            else -> error("Unrecognized track type")
          }
        }
      } catch (e: NullPointerException) {
        Log.e(TAG, "Couldn't load tracks, probably cause mpv was destroyed")
        return@launch
      }
      _subtitleTracks.update { subTracks }
      _selectedSubtitles.update { Pair(activity.player.sid, activity.player.secondarySid) }
      _audioTracks.update { audioTracks }
      _selectedAudio.update { activity.player.aid }
    }
  }

  fun selectSub(id: Int) {
    val selectedSubs = selectedSubtitles.value
    _selectedSubtitles.update {
      when (id) {
        selectedSubs.first -> Pair(selectedSubs.second, -1)
        selectedSubs.second -> Pair(selectedSubs.first, -1)
        else -> {
          if (selectedSubs.first != -1) {
            Pair(selectedSubs.first, id)
          } else {
            Pair(id, -1)
          }
        }
      }
    }
    activity.player.secondarySid = _selectedSubtitles.value.second
    activity.player.sid = _selectedSubtitles.value.first
  }

  fun loadChapters() {
    val chapters = mutableListOf<Segment>()
    val count = MPVLib.getPropertyInt("chapter-list/count")!!
    for (i in 0 until count) {
      val title = MPVLib.getPropertyString("chapter-list/$i/title")
      val time = MPVLib.getPropertyDouble("chapter-list/$i/time")!!
      chapters.add(
        Segment(
          name = title,
          start = time.toFloat(),
        ),
      )
    }
    this.chapters = chapters.sortedBy { it.start }.filter { it.start in 0f..duration.value }
  }

  fun selectChapter(index: Int) {
    val time = chapters[index].start
    seekTo(time.toInt())
  }

  fun updateChapter(index: Long) {
    if (chapters.isEmpty() || index == -1L) return
    _currentChapter.update { chapters.getOrNull(index.toInt()) ?: return }
  }

  fun selectAudio(id: Int) {
    activity.player.aid = id
    _selectedAudio.update { id }
  }

  fun addSubtitle(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) {
      activity.openContentFd(Uri.parse(url))
    } else {
      url
    } ?: return
    MPVLib.command(arrayOf("sub-add", path, "cached"))
  }

  fun setSubtitle(sid: Int, secondarySid: Int) {
    _selectedSubtitles.update { Pair(sid, secondarySid) }
  }

  fun addAudio(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) {
      activity.openContentFd(Uri.parse(url))
    } else {
      url
    } ?: return
    val trackCount = MPVLib.getPropertyInt("track-list/count")
    MPVLib.command(arrayOf("audio-add", path, "cached"))
    if (trackCount == MPVLib.getPropertyInt("track-list/count")) return
    _audioTracks.update { it.plus(Track(activity.player.aid, path, null)) }
    _selectedAudio.update { activity.player.aid }
  }

  fun updatePlayBackPos(pos: Float) {
    _pos.update { pos }
  }

  fun updateReadAhead(value: Long) {
    _readAhead.update { value.toFloat() }
  }

  fun pauseUnpause() {
    if (paused.value) {
      unpause()
    } else {
      pause()
    }
  }

  fun pause() {
    activity.player.paused = true
    _paused.update { true }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching {
        activity.setPictureInPictureParams(activity.createPipParams())
      }
    }
  }

  fun unpause() {
    activity.player.paused = false
    _paused.update { false }
  }

  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    _controlsShown.update { true }
  }

  fun hideControls() {
    _controlsShown.update { false }
  }

  fun hideSeekBar() {
    _seekBarShown.update { false }
  }

  fun showSeekBar() {
    if (sheetShown.value != Sheets.None) return
    _seekBarShown.update { true }
  }

  fun lockControls() {
    _areControlsLocked.update { true }
  }

  fun unlockControls() {
    _areControlsLocked.update { false }
  }

  fun seekBy(offset: Int) {
    MPVLib.command(arrayOf("seek", offset.toString(), "relative"))
    isLoading.update { true }
  }

  fun seekTo(position: Int) {
    if (position < 0) return
    if (position > (activity.player.duration ?: 0)) return
    activity.player.timePos = position
    isLoading.update { true }
  }

  fun changeBrightnessBy(change: Float) {
    changeBrightnessTo(currentBrightness.value + change)
  }

  fun changeBrightnessTo(
    brightness: Float,
  ) {
    isBrightnessSliderShown.update { sheetShown.value == Sheets.None }
    currentBrightness.update { brightness.coerceIn(0f, 1f) }
    activity.window.attributes = activity.window.attributes.apply {
      screenBrightness = brightness.coerceIn(0f, 1f)
    }
  }

  fun changeVolumeBy(change: Int) {
    changeVolumeTo(currentVolume.value + change)
  }

  val maxVolume = activity.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
  fun changeVolumeTo(volume: Int) {
    isVolumeSliderShown.update { sheetShown.value == Sheets.None }
    val newVolume = volume.coerceIn(0..maxVolume)
    activity.audioManager.setStreamVolume(
      AudioManager.STREAM_MUSIC,
      newVolume,
      0,
    )
    currentVolume.update { newVolume }
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
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getRealMetrics(dm)
        ratio = dm.widthPixels / dm.heightPixels.toDouble()
        pan = 0.0
      }
    }
    MPVLib.setPropertyDouble("panscan", pan)
    MPVLib.setPropertyDouble("video-aspect-override", ratio)
    playerPreferences.videoAspect.set(aspect)
    playerUpdate.update { PlayerUpdates.AspectRatio }
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activity.setPictureInPictureParams(activity.createPipParams())
      }
    }
  }

  fun cycleScreenRotations() {
    activity.requestedOrientation = when (activity.requestedOrientation) {
      ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
      -> {
        playerPreferences.orientation.set(PlayerOrientation.SensorPortrait)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      }

      else -> {
        playerPreferences.orientation.set(PlayerOrientation.SensorLandscape)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }
  }
}

data class Track(
  val id: Int,
  val name: String,
  val language: String?,
)

fun Float.normalize(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
  return (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
}
