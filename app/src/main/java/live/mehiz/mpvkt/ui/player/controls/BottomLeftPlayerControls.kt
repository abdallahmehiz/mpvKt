package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.CurrentChapter
import org.koin.compose.koinInject

@Composable
fun BottomLeftPlayerControls(modifier: Modifier = Modifier) {
  val viewModel = koinInject<PlayerViewModel>()
  val playerPreferences = koinInject<PlayerPreferences>()
  val currentChapter by viewModel.currentChapter.collectAsState()
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ControlsButton(
      Icons.Default.LockOpen,
      onClick = { viewModel.lockControls() },
    )
    ControlsButton(
      icon = Icons.Default.ScreenRotation,
      onClick = { viewModel.cycleScreenRotations() }
    )
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    ControlsButton(
      text = stringResource(R.string.player_speed, currentSpeed),
      onClick = {
        val newSpeed = if (currentSpeed >= 2) 0.25f else currentSpeed + 0.25f
        viewModel.playbackSpeed.update { newSpeed }
        MPVLib.setPropertyDouble("speed", newSpeed.toDouble())
        playerPreferences.defaultSpeed.set(newSpeed)
      },
      onLongClick = { viewModel.sheetShown.update { Sheets.PlaybackSpeed } }
    )
    AnimatedVisibility(
      currentChapter != null && playerPreferences.currentChaptersIndicator.get(),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      CurrentChapter(
        currentChapter!!,
        onClick = { viewModel.sheetShown.update { Sheets.Chapters } },
      )
    }
  }
}
