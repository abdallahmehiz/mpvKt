package live.mehiz.mpvkt.ui.player

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.domain.custombuttons.repository.CustomButtonRepository
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsUiState
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import live.mehiz.mpvkt.ui.player.manager.LuaManager
import live.mehiz.mpvkt.ui.player.manager.PlayerManager

class PlayerViewModelFactory(
  private val playerManager: PlayerManager,
  private val initialBrightness: Float,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    require(modelClass.isAssignableFrom(PlayerViewModel::class.java)) { "Unknown ViewModel class" }
    @Suppress("UNCHECKED_CAST")
    return PlayerViewModel(
      playerManager = playerManager,
      customButtonRepository = App.graph.customButtonRepository,
      playerPreferences = App.graph.playerPreferences,
      gesturePreferences = App.graph.gesturePreferences,
      initialBrightness = initialBrightness,
    ) as T
  }
}

@Suppress("TooManyFunctions")
@Stable // none of the values in the viewmodel change randomly, ig
class PlayerViewModel(
  val playerManager: PlayerManager,
  private val customButtonRepository: CustomButtonRepository,
  private val playerPreferences: PlayerPreferences,
  private val gesturePreferences: GesturePreferences,
  initialBrightness: Float,
) : ViewModel() {
  private val playback = playerManager.playback

  private val eventChannel = Channel<Event>(Channel.BUFFERED)
  val eventFlow = eventChannel.receiveAsFlow()

  private val _customButtons = MutableStateFlow<CustomButtonsUiState>(CustomButtonsUiState.Loading)
  val customButtons = _customButtons.asStateFlow()

  private val _primaryButton = MutableStateFlow<CustomButtonEntity?>(null)
  val primaryButton = _primaryButton.asStateFlow()

  private val _primaryButtonTitle = MutableStateFlow("")
  val primaryButtonTitle = _primaryButtonTitle.asStateFlow()

  init {
    playerManager.lua.events
      .onEach { event ->
        when (event) {
          is LuaManager.Event.Seek -> showLuaSeek(event)
          is LuaManager.Event.Ui -> handleLuaCommand(event.command, event.data)
        }
      }
      .launchIn(viewModelScope)

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val buttons = customButtonRepository.getCustomButtons().first()
        primaryCustomButton(buttons)?.let {
          _primaryButton.update { _ -> it }
          // If the button text is not empty, it has been set buy a lua script in which
          // case we don't want to override it
          if (_primaryButtonTitle.value.isEmpty()) setPrimaryCustomButtonTitle(it)
          eventChannel.send(Event.SetupCustomButtons(buttons))
        }
        _customButtons.update { _ -> CustomButtonsUiState.Success(buttons) }
      } catch (e: Exception) {
        Log.e(TAG, e.message ?: "Unable to fetch buttons")
        _customButtons.update { _ -> CustomButtonsUiState.Error(e.message ?: "Unable to fetch buttons") }
      }
    }
  }

  private fun primaryCustomButton(buttons: List<CustomButtonEntity>) =
    buttons.firstOrNull { it.id == playerPreferences.primaryCustomButtonId.get() }

  fun setPrimaryCustomButtonTitle(button: CustomButtonEntity) {
    _primaryButtonTitle.update { _ -> button.title }
  }

  private val _controlsShown = MutableStateFlow(true)
  val controlsShown = _controlsShown.asStateFlow()
  private val _seekBarShown = MutableStateFlow(true)
  val seekBarShown = _seekBarShown.asStateFlow()
  private val _areControlsLocked = MutableStateFlow(false)
  val areControlsLocked = _areControlsLocked.asStateFlow()

  val playerUpdate = MutableStateFlow<PlayerUpdates>(PlayerUpdates.None)
  val sheetShown = MutableStateFlow(Sheets.None)
  val panelShown = MutableStateFlow(Panels.None)

  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    eventChannel.trySend(Event.SetControls(true))
    _controlsShown.update { true }
  }

  fun hideControls() {
    eventChannel.trySend(Event.SetControls(false))
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

  fun pauseUnpause() = playback.togglePause()
  fun pause() = playback.setPaused(true)
  fun unpause() = playback.setPaused(false)

  fun seekTo(position: Int, precise: Boolean = true) = playback.seekTo(position, precise)

  val isBrightnessSliderShown = MutableStateFlow(false)
  val isVolumeSliderShown = MutableStateFlow(false)
  val currentBrightness = MutableStateFlow(initialBrightness)

  fun changeBrightnessBy(change: Float) {
    changeBrightnessTo(currentBrightness.value + change)
  }

  fun changeBrightnessTo(brightness: Float) {
    val coercedBrightness = brightness.coerceIn(0f, 1f)
    currentBrightness.update { _ -> coercedBrightness }
    eventChannel.trySend(Event.ChangeBrightness(coercedBrightness))
  }

  fun displayBrightnessSlider() {
    isBrightnessSliderShown.update { true }
  }

  val currentVolume = playerManager.audio.streamVolume
  val maxVolume = playerManager.audio.maxVolume

  fun changeVolumeBy(change: Int) = playerManager.audio.changeVolumeBy(change)
  fun changeVolumeTo(volume: Int) = playerManager.audio.setStreamVolume(volume)
  fun changeMPVVolumeTo(volume: Int) = playerManager.audio.setMpvVolume(volume)

  fun displayVolumeSlider() {
    isVolumeSliderShown.update { true }
  }

  fun changeVideoAspect(aspect: VideoAspect) {
    eventChannel.trySend(Event.ApplyVideoAspect(aspect))
    playerUpdate.update { PlayerUpdates.AspectRatio }
  }

  fun cycleScreenRotations() {
    eventChannel.trySend(Event.CycleRotation)
  }

  // Pair(startingPosition, seekAmount)
  val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)

  private val _seekText = MutableStateFlow<String?>(null)
  val seekText = _seekText.asStateFlow()
  private val _doubleTapSeekAmount = MutableStateFlow(0)
  val doubleTapSeekAmount = _doubleTapSeekAmount.asStateFlow()
  private val _isSeekingForwards = MutableStateFlow(false)
  val isSeekingForwards = _isSeekingForwards.asStateFlow()

  private val doubleTapToSeekDuration = gesturePreferences.doubleTapToSeekDuration.get()

  fun updateSeekAmount(amount: Int) {
    _doubleTapSeekAmount.update { amount }
  }

  fun updateSeekText(text: String?) {
    _seekText.update { text }
  }

  private fun afterSeek() {
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  private fun showLuaSeek(seek: LuaManager.Event.Seek) {
    _isSeekingForwards.value = seek.offset > 0
    _doubleTapSeekAmount.update {
      when {
        seek.absolute -> seek.offset
        seek.offset < 0 && it < 0 || playback.position.value + seek.offset > playback.duration.value -> 0
        else -> it + seek.offset
      }
    }
    _seekText.update { seek.text }
    afterSeek()
  }

  fun leftSeek() {
    if (playback.position.value > 0) _doubleTapSeekAmount.value -= doubleTapToSeekDuration
    _isSeekingForwards.value = false
    playback.seekBy(-doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    afterSeek()
  }

  fun rightSeek() {
    if (playback.position.value < playback.duration.value) _doubleTapSeekAmount.value += doubleTapToSeekDuration
    _isSeekingForwards.value = true
    playback.seekBy(doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    afterSeek()
  }

  private fun runGesture(gesture: SingleActionGesture, seek: () -> Unit, key: CustomKeyCodes) {
    when (gesture) {
      SingleActionGesture.Seek -> seek()
      SingleActionGesture.PlayPause -> pauseUnpause()
      SingleActionGesture.Custom -> playerManager.keypress(key.keyCode)
      SingleActionGesture.None -> {}
    }
  }

  fun handleLeftDoubleTap() =
    runGesture(gesturePreferences.leftSingleActionGesture.get(), ::leftSeek, CustomKeyCodes.DoubleTapLeft)

  fun handleCenterDoubleTap() =
    runGesture(gesturePreferences.centerSingleActionGesture.get(), {}, CustomKeyCodes.DoubleTapCenter)

  fun handleRightDoubleTap() =
    runGesture(gesturePreferences.rightSingleActionGesture.get(), ::rightSeek, CustomKeyCodes.DoubleTapRight)

  @Suppress("CyclomaticComplexMethod")
  private fun handleLuaCommand(command: String, data: String) {
    when (command) {
      "show_text" -> playerUpdate.update { PlayerUpdates.ShowText(data) }

      "toggle_ui" -> when (data) {
        "show" -> showControls()
        "toggle" -> if (controlsShown.value) hideControls() else showControls()
        "hide" -> {
          sheetShown.update { Sheets.None }
          panelShown.update { Panels.None }
          hideControls()
        }
      }

      "show_panel" -> when (data) {
        "subtitle_settings" -> panelShown.update { Panels.SubtitleSettings }
        "subtitle_delay" -> panelShown.update { Panels.SubtitleDelay }
        "audio_delay" -> panelShown.update { Panels.AudioDelay }
        "video_filters" -> panelShown.update { Panels.VideoFilters }
      }

      "set_button_title" -> _primaryButtonTitle.update { _ -> data }

      "reset_button_title" -> primaryCustomButton(customButtons.value.getButtons())?.let(::setPrimaryCustomButtonTitle)

      "toggle_button" -> {
        fun showButton() {
          if (_primaryButton.value == null) _primaryButton.update { primaryCustomButton(customButtons.value.getButtons()) }
        }
        when (data) {
          "show" -> showButton()
          "toggle" if _primaryButton.value != null -> showButton()
          else -> _primaryButton.update { null }
        }
      }

      "software_keyboard" -> when (data) {
        "show" -> eventChannel.trySend(Event.SoftwareKeyboard(KeyboardAction.Show))
        "hide" -> eventChannel.trySend(Event.SoftwareKeyboard(KeyboardAction.Hide))
        "toggle" -> eventChannel.trySend(Event.SoftwareKeyboard(KeyboardAction.Toggle))
      }
    }
  }

  enum class KeyboardAction { Show, Hide, Toggle }

  sealed interface Event {
    data object CycleRotation : Event
    data class SetControls(val visible: Boolean) : Event
    data class ApplyVideoAspect(val aspect: VideoAspect) : Event
    data class ChangeBrightness(val brightness: Float) : Event
    data class SetupCustomButtons(val buttons: List<CustomButtonEntity>) : Event
    data class SoftwareKeyboard(val action: KeyboardAction) : Event
  }
}

fun Float.normalize(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float =
  (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
