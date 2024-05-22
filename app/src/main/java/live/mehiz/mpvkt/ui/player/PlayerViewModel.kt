package live.mehiz.mpvkt.ui.player

import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(
  private val activity: PlayerActivity
): ViewModel() {
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

  fun updatePlayBackPos(pos: Float) {
    _pos.value = pos
  }

  fun updateReadAhead(value: Long) {
    _readAhead.value = pos.value + value.toFloat()
  }

  fun pauseUnpause() {
    if(paused.value) unpause()
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
    if(controlsShown.value) hideControls()
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
}
