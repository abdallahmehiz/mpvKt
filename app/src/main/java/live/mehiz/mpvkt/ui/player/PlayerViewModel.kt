package live.mehiz.mpvkt.ui.player

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(
  private val activity: PlayerActivity
): ViewModel() {

  private val _pos = MutableStateFlow(0f)
  val pos = _pos.asStateFlow()

  fun updatePlayBackPos(pos: Float) {
    _pos.value = pos
  }
}
