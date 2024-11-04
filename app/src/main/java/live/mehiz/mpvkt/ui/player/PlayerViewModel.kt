package live.mehiz.mpvkt.ui.player

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.MpvKtDatabase
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsUiState
import org.koin.java.KoinJavaComponent.inject
import java.io.File

@Suppress("TooManyFunctions")
class PlayerViewModel(
  private val activity: PlayerActivity,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)
  private val gesturePreferences: GesturePreferences by inject(GesturePreferences::class.java)
  private val mpvKtDatabase: MpvKtDatabase by inject(MpvKtDatabase::class.java)

  private val _customButtons = MutableStateFlow<CustomButtonsUiState>(CustomButtonsUiState.Loading)
  val customButtons = _customButtons.asStateFlow()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        mpvKtDatabase.customButtonDao().getCustomButtons()
          .catch { e ->
            Log.e(TAG, e.message ?: "Unable to fetch buttons")
            _customButtons.update { _ -> CustomButtonsUiState.Error(e.message ?: "Unable to fetch buttons") }
          }
          .collectLatest { buttons ->
            _customButtons.update { _ -> CustomButtonsUiState.Success(buttons) }
          }
      } catch (e: Exception) {
        Log.e(TAG, e.message ?: "Unable to fetch buttons")
        _customButtons.update { _ -> CustomButtonsUiState.Error(e.message ?: "Unable to fetch buttons") }
      }
    }
  }

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
    runCatching {
      Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        .normalize(0f, 255f, 0f, 1f)
    }.getOrElse { 0f },
  )
  val currentVolume = MutableStateFlow(activity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
  val currentMPVVolume = MutableStateFlow(MPVLib.getPropertyInt("volume"))
  var volumeBoostCap: Int = MPVLib.getPropertyInt("volume-max")

  val sheetShown = MutableStateFlow(Sheets.None)
  val panelShown = MutableStateFlow(Panels.None)

  // Pair(startingPosition, seekAmount)
  val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)

  private val _doubleTapSeekAmount = MutableStateFlow(0)
  val doubleTapSeekAmount = _doubleTapSeekAmount.asStateFlow()
  private val _isSeekingForwards = MutableStateFlow(false)
  val isSeekingForwards = _isSeekingForwards.asStateFlow()

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
      _audioTracks.update { audioTracks }
    }
  }

  fun loadChapters() {
    val chapters = mutableListOf<Segment>()
    val count = MPVLib.getPropertyInt("chapter-list/count")!!
    for (i in 0 until count) {
      val title = MPVLib.getPropertyString("chapter-list/$i/title")
      val time = MPVLib.getPropertyInt("chapter-list/$i/time")!!
      chapters.add(
        Segment(
          name = title,
          start = time.toFloat(),
        ),
      )
    }
    this.chapters = chapters.sortedBy { it.start }
  }

  fun selectChapter(index: Int) {
    val time = chapters[index].start
    seekTo(time.toInt())
  }

  fun updateChapter(index: Long) {
    if (chapters.isEmpty() || index == -1L) return
    _currentChapter.update { chapters.getOrNull(index.toInt()) ?: return }
  }

  fun addAudio(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) {
      Uri.parse(url).openContentFd(activity)
    } else {
      url
    } ?: return
    MPVLib.command(arrayOf("audio-add", path, "cached"))
  }

  fun selectAudio(id: Int) {
    activity.player.aid = id
  }

  fun updateAudio(id: Int) {
    _selectedAudio.update { id }
  }

  fun addSubtitle(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) {
      Uri.parse(url).openContentFd(activity)
    } else {
      url
    } ?: return
    MPVLib.command(arrayOf("sub-add", path, "cached"))
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

  fun updateSubtitle(sid: Int, secondarySid: Int) {
    _selectedSubtitles.update { Pair(sid, secondarySid) }
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

  private val showStatusBar = playerPreferences.showSystemStatusBar.get()
  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    if (showStatusBar) {
      activity.windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    }
    _controlsShown.update { true }
  }

  fun hideControls() {
    activity.windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
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

  fun seekBy(offset: Int, precise: Boolean = false) {
    MPVLib.command(arrayOf("seek", offset.toString(), if (precise) "relative+exact" else "relative"))
  }

  fun seekTo(position: Int, precise: Boolean = true) {
    if (position !in 0..(activity.player.duration ?: 0)) return
    MPVLib.command(arrayOf("seek", position.toString(), if (precise) "absolute" else "absolute+keyframes"))
  }

  fun changeBrightnessBy(change: Float) {
    changeBrightnessTo(currentBrightness.value + change)
  }

  fun changeBrightnessTo(
    brightness: Float,
  ) {
    activity.window.attributes = activity.window.attributes.apply {
      screenBrightness = brightness.coerceIn(0f, 1f).also {
        currentBrightness.update { _ -> it }
      }
    }
  }

  fun displayBrightnessSlider() {
    isBrightnessSliderShown.update { true }
  }

  val maxVolume = activity.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
  fun changeVolumeBy(change: Int) {
    val mpvVolume = MPVLib.getPropertyInt("volume")
    if (volumeBoostCap > 0 && currentVolume.value == maxVolume) {
      if (mpvVolume == 100 && change < 0) changeVolumeTo(currentVolume.value + change)
      val finalMPVVolume = (mpvVolume + change).coerceAtLeast(100)
      if (finalMPVVolume in 100..volumeBoostCap + 100) {
        changeMPVVolumeTo(finalMPVVolume)
        return
      }
    }
    changeVolumeTo(currentVolume.value + change)
  }

  fun changeVolumeTo(volume: Int) {
    val newVolume = volume.coerceIn(0..maxVolume)
    activity.audioManager.setStreamVolume(
      AudioManager.STREAM_MUSIC,
      newVolume,
      0,
    )
    currentVolume.update { newVolume }
  }

  fun changeMPVVolumeTo(volume: Int) {
    MPVLib.setPropertyInt("volume", volume)
  }

  fun setMPVVolume(volume: Int) {
    if (volume != currentMPVVolume.value) displayVolumeSlider()
    currentMPVVolume.update { volume }
  }

  fun displayVolumeSlider() {
    isVolumeSliderShown.update { true }
  }

  fun changeVideoAspect(aspect: VideoAspect) {
    var ratio = -1.0
    var pan = 1.0
    when (aspect) {
      VideoAspect.Crop -> {
        pan = 1.0
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

  private val doubleTapToSeekDuration = gesturePreferences.doubleTapToSeekDuration.get()

  fun updateSeekAmount(amount: Int) {
    _doubleTapSeekAmount.update { _ -> amount }
  }

  fun handleLeftDoubleTap() {
    when (gesturePreferences.leftDoubleTapGesture.get()) {
      DoubleTapGesture.Seek -> {
        if (pos.value > 0) {
          _doubleTapSeekAmount.value -= doubleTapToSeekDuration
        }
        _isSeekingForwards.value = false
        seekBy(-doubleTapToSeekDuration)
        if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
      }
      DoubleTapGesture.PlayPause -> {
        pauseUnpause()
      }
      DoubleTapGesture.Custom -> {
        MPVLib.command(arrayOf("keypress", CustomKeyCodes.Left.keyCode))
      }
      DoubleTapGesture.None -> {}
    }
  }

  fun handleCenterDoubleTap() {
    when (gesturePreferences.centerDoubleTapGesture.get()) {
      DoubleTapGesture.PlayPause -> {
        pauseUnpause()
      }
      DoubleTapGesture.Custom -> {
        MPVLib.command(arrayOf("keypress", CustomKeyCodes.Center.keyCode))
      }
      DoubleTapGesture.Seek -> {}
      DoubleTapGesture.None -> {}
    }
  }

  fun handleRightDoubleTap() {
    when (gesturePreferences.rightDoubleTapGesture.get()) {
      DoubleTapGesture.Seek -> {
        if (pos.value < duration.value) {
          _doubleTapSeekAmount.value += doubleTapToSeekDuration
        }
        _isSeekingForwards.value = true
        seekBy(doubleTapToSeekDuration)
        if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
      }
      DoubleTapGesture.PlayPause -> {
        pauseUnpause()
      }
      DoubleTapGesture.Custom -> {
        MPVLib.command(arrayOf("keypress", CustomKeyCodes.Right.keyCode))
      }
      DoubleTapGesture.None -> {}
    }
  }

  fun executeCustomButton(button: CustomButtonEntity) {
    val tempFile = File.createTempFile("script", ".lua").apply {
      writeText(button.content)
      deleteOnExit()
    }

    MPVLib.command(arrayOf("load-script", tempFile.absolutePath))
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
