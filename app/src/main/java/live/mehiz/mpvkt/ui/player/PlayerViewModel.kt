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

  private val _controlsShown = MutableStateFlow(true)
  val controlsShown = _controlsShown.asStateFlow()

  fun updatePlayBackPos(pos: Float) {
    _pos.value = pos
  }

  fun showControls() {
    _controlsShown.value = true
    activity.windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
  }
  fun hideControls() {
    _controlsShown.value = false
    activity.windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
  }
  fun toggleControls() {
    if(controlsShown.value) hideControls()
    else showControls()
  }
}
