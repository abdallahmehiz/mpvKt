package live.mehiz.mpvkt.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.MpvKtDatabase
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsUiState
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import org.koin.java.KoinJavaComponent.inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PlayerViewModelProviderFactory(
  private val activity: PlayerActivity,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
    return PlayerViewModel(activity) as T
  }
}

@Suppress("TooManyFunctions")
class PlayerViewModel(
  private val activity: PlayerActivity,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)
  private val gesturePreferences: GesturePreferences by inject(GesturePreferences::class.java)
  private val audioPreferences: AudioPreferences by inject(AudioPreferences::class.java)
  private val mpvKtDatabase: MpvKtDatabase by inject(MpvKtDatabase::class.java)
  private val json: Json by inject(Json::class.java)

  init {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val buttons = mpvKtDatabase.customButtonDao().getCustomButtons().first()
        buttons.firstOrNull { it.id == playerPreferences.primaryCustomButtonId.get() }?.let {
          _primaryButton.update { _ -> it }
          // If the button text is not empty, it has been set buy a lua script in which
          // case we don't want to override it
          if (_primaryButtonTitle.value.isEmpty()) setPrimaryCustomButtonTitle(it)
        }
        activity.setupCustomButtons(buttons)
        _customButtons.update { _ -> CustomButtonsUiState.Success(buttons) }
      } catch (e: Exception) {
        Log.e(TAG, e.message ?: "Unable to fetch buttons")
        _customButtons.update { _ -> CustomButtonsUiState.Error(e.message ?: "Unable to fetch buttons") }
      }
    }
  }

  private val _customButtons = MutableStateFlow<CustomButtonsUiState>(CustomButtonsUiState.Loading)
  val customButtons = _customButtons.asStateFlow()

  private val _primaryButton = MutableStateFlow<CustomButtonEntity?>(null)
  val primaryButton = _primaryButton.asStateFlow()

  private val _primaryButtonTitle = MutableStateFlow("")
  val primaryButtonTitle = _primaryButtonTitle.asStateFlow()

  val paused by MPVLib.propBoolean["pause"].collectAsState(viewModelScope)
  val pos by MPVLib.propInt["time-pos"].collectAsState(viewModelScope)
  val duration by MPVLib.propInt["duration"].collectAsState(viewModelScope)
  private val currentMPVVolume by MPVLib.propInt["volume"].collectAsState(viewModelScope)

  val currentVolume = MutableStateFlow(activity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
  private val volumeBoostCap by MPVLib.propInt["volume-max"].collectAsState(viewModelScope)

  val subtitleTracks = MPVLib.propNode["track-list"]
    .map { (it?.toObject<List<TrackNode>>(json)?.filter { it.isSubtitle } ?: persistentListOf()).toImmutableList() }

  val audioTracks = MPVLib.propNode["track-list"]
    .map { (it?.toObject<List<TrackNode>>(json)?.filter { it.isAudio } ?: persistentListOf()).toImmutableList() }

  val chapters = MPVLib.propNode["chapter-list"]
    .map { (it?.toObject<List<ChapterNode>>(json) ?: persistentListOf()).map { it.toSegment() }.toImmutableList() }

  private val _controlsShown = MutableStateFlow(true)
  val controlsShown = _controlsShown.asStateFlow()
  private val _seekBarShown = MutableStateFlow(true)
  val seekBarShown = _seekBarShown.asStateFlow()
  private val _areControlsLocked = MutableStateFlow(false)
  val areControlsLocked = _areControlsLocked.asStateFlow()

  val playerUpdate = MutableStateFlow<PlayerUpdates>(PlayerUpdates.None)
  val isBrightnessSliderShown = MutableStateFlow(false)
  val isVolumeSliderShown = MutableStateFlow(false)
  val currentBrightness = MutableStateFlow(
    runCatching {
      Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        .normalize(0f, 255f, 0f, 1f)
    }.getOrElse { 0f },
  )

  val sheetShown = MutableStateFlow(Sheets.None)
  val panelShown = MutableStateFlow(Panels.None)

  // Pair(startingPosition, seekAmount)
  val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)

  private val _seekText = MutableStateFlow<String?>(null)
  val seekText = _seekText.asStateFlow()
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
      MPVLib.setPropertyBoolean("pause", true)
      Toast.makeText(activity, activity.getString(R.string.toast_sleep_timer_ended), Toast.LENGTH_SHORT).show()
    }
  }

  fun cycleDecoders() {
    MPVLib.setPropertyString(
      "hwdec",
      when (Decoder.getDecoderFromValue(MPVLib.getPropertyString("hwdec-current") ?: return)) {
        Decoder.HWPlus -> Decoder.HW.value
        Decoder.HW -> Decoder.SW.value
        Decoder.SW -> Decoder.HWPlus.value
        Decoder.AutoCopy -> Decoder.SW.value
        Decoder.Auto -> Decoder.SW.value
      },
    )
  }

  fun addAudio(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) url.toUri().openContentFd(activity) else url
    MPVLib.command("audio-add", path ?: return, "cached")
  }

  fun addSubtitle(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) url.toUri().openContentFd(activity) else url
    MPVLib.command("sub-add", path ?: return, "cached")
  }

  fun selectSub(id: Int) {
    val selectedSubs = Pair(MPVLib.getPropertyInt("sid"), MPVLib.getPropertyInt("secondary-sid"))
    when (id) {
      selectedSubs.first -> Pair(selectedSubs.second, null)
      selectedSubs.second -> Pair(selectedSubs.first, null)
      else -> if (selectedSubs.first != null) Pair(selectedSubs.first, id) else Pair(id, null)
    }.let {
      it.second?.let { MPVLib.setPropertyInt("secondary-sid", it) } ?: MPVLib.setPropertyBoolean("secondary-sid", false)
      it.first?.let { MPVLib.setPropertyInt("sid", it) } ?: MPVLib.setPropertyBoolean("sid", false)
    }
  }

  fun pauseUnpause() = MPVLib.command("cycle", "pause")
  fun pause() = MPVLib.setPropertyBoolean("pause", true)
  fun unpause() = MPVLib.setPropertyBoolean("pause", false)

  private val showStatusBar = playerPreferences.showSystemStatusBar.get()
  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    if (showStatusBar) activity.windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
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
    MPVLib.command("seek", offset.toString(), if (precise) "relative+exact" else "relative")
  }

  fun seekTo(position: Int, precise: Boolean = true) {
    if (position !in 0..(MPVLib.getPropertyInt("duration") ?: 0)) return
    MPVLib.command("seek", position.toString(), if (precise) "absolute" else "absolute+keyframes")
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
    if ((volumeBoostCap ?: audioPreferences.volumeBoostCap.get()) > 0 && currentVolume.value == maxVolume) {
      if (mpvVolume == 100 && change < 0) changeVolumeTo(currentVolume.value + change)
      val finalMPVVolume = (mpvVolume?.plus(change))?.coerceAtLeast(100) ?: 100
      if (finalMPVVolume in 100..(volumeBoostCap ?: audioPreferences.volumeBoostCap.get()) + 100) {
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
    if (volume != currentMPVVolume) displayVolumeSlider()
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

  @Suppress("CyclomaticComplexMethod", "LongMethod")
  fun handleLuaInvocation(property: String, value: String) {
    val data = value
      .removePrefix("\"")
      .removeSuffix("\"")
      .ifEmpty { return }

    when (property.substringAfterLast("/")) {
      "show_text" -> playerUpdate.update { PlayerUpdates.ShowText(data) }
      "toggle_ui" -> {
        when (data) {
          "show" -> showControls()
          "toggle" -> {
            if (controlsShown.value) hideControls() else showControls()
          }

          "hide" -> {
            sheetShown.update { Sheets.None }
            panelShown.update { Panels.None }
            hideControls()
          }
        }
      }

      "show_panel" -> {
        when (data) {
          "subtitle_settings" -> panelShown.update { Panels.SubtitleSettings }
          "subtitle_delay" -> panelShown.update { Panels.SubtitleDelay }
          "audio_delay" -> panelShown.update { Panels.AudioDelay }
          "video_filters" -> panelShown.update { Panels.VideoFilters }
        }
      }

      "set_button_title" -> {
        _primaryButtonTitle.update { _ -> data }
      }

      "reset_button_title" -> {
        _customButtons.value.getButtons().firstOrNull { it.id == playerPreferences.primaryCustomButtonId.get() }?.let {
          setPrimaryCustomButtonTitle(it)
        }
      }

      "seek_to_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekToWithText(seekValue.toInt(), text)
      }

      "seek_by_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekByWithText(seekValue.toInt(), text)
      }

      "seek_by" -> seekByWithText(data.toInt(), null)
      "seek_to" -> seekToWithText(data.toInt(), null)
      "toggle_button" -> {
        fun showButton() {
          if (_primaryButton.value == null) {
            _primaryButton.update {
              customButtons.value.getButtons().firstOrNull { it.id == playerPreferences.primaryCustomButtonId.get() }
            }
          }
        }
        when (data) {
          "show" -> showButton()
          "toggle" if _primaryButton.value != null -> showButton()
          else -> _primaryButton.update { null }
        }
      }

      "software_keyboard" -> when (data) {
        "show" -> forceShowSoftwareKeyboard()
        "hide" -> forceHideSoftwareKeyboard()
        "toggle" if !inputMethodManager.isActive -> forceShowSoftwareKeyboard()
        else -> forceHideSoftwareKeyboard()
      }
    }

    MPVLib.setPropertyString(property, "")
  }

  private val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  private fun forceShowSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
  }

  private fun forceHideSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
  }

  private fun seekToWithText(seekValue: Int, text: String?) {
    _isSeekingForwards.value = seekValue > 0
    _doubleTapSeekAmount.value = seekValue - (pos ?: return)
    _seekText.update { text }
    seekTo(seekValue, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  private fun seekByWithText(value: Int, text: String?) {
    _doubleTapSeekAmount.update {
      if (value < 0 && it < 0 || (pos ?: return) + value > (duration ?: return)) 0 else it + value
    }
    _seekText.update { text }
    _isSeekingForwards.value = value > 0
    seekBy(value, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  private val doubleTapToSeekDuration = gesturePreferences.doubleTapToSeekDuration.get()

  fun updateSeekAmount(amount: Int) {
    _doubleTapSeekAmount.update { amount }
  }

  fun updateSeekText(text: String?) {
    _seekText.update { text }
  }

  fun leftSeek() {
    if ((pos ?: return) > 0) _doubleTapSeekAmount.value -= doubleTapToSeekDuration
    _isSeekingForwards.value = false
    seekBy(-doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun rightSeek() {
    if ((pos ?: return) < (duration ?: return)) {
      _doubleTapSeekAmount.value += doubleTapToSeekDuration
    }
    _isSeekingForwards.value = true
    seekBy(doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun handleLeftDoubleTap() {
    when (gesturePreferences.leftSingleActionGesture.get()) {
      SingleActionGesture.Seek -> {
        leftSeek()
      }

      SingleActionGesture.PlayPause -> {
        pauseUnpause()
      }

      SingleActionGesture.Custom -> {
        MPVLib.command("keypress", CustomKeyCodes.DoubleTapLeft.keyCode)
      }

      SingleActionGesture.None -> {}
    }
  }

  fun handleCenterDoubleTap() {
    when (gesturePreferences.centerSingleActionGesture.get()) {
      SingleActionGesture.PlayPause -> {
        pauseUnpause()
      }

      SingleActionGesture.Custom -> {
        MPVLib.command("keypress", CustomKeyCodes.DoubleTapCenter.keyCode)
      }

      SingleActionGesture.Seek -> {}
      SingleActionGesture.None -> {}
    }
  }

  fun handleRightDoubleTap() {
    when (gesturePreferences.rightSingleActionGesture.get()) {
      SingleActionGesture.Seek -> {
        rightSeek()
      }

      SingleActionGesture.PlayPause -> {
        pauseUnpause()
      }

      SingleActionGesture.Custom -> {
        MPVLib.command("keypress", CustomKeyCodes.DoubleTapRight.keyCode)
      }

      SingleActionGesture.None -> {}
    }
  }

  fun setPrimaryCustomButtonTitle(button: CustomButtonEntity) {
    _primaryButtonTitle.update { _ -> button.title }
  }
}

fun Float.normalize(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
  return (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
}

fun CustomButtonEntity.execute() {
  MPVLib.command("script-message", "call_button_$id")
}

fun CustomButtonEntity.executeLongClick() {
  MPVLib.command("script-message", "call_button_${id}_long")
}

fun <T> Flow<T>.collectAsState(scope: CoroutineScope, initialValue: T? = null) =
  object : ReadOnlyProperty<Any?, T?> {
    private var value: T? = initialValue
    init { scope.launch { collect { value = it } } }
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
  }
