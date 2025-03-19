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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vivvvek.seeker.Segment
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.CurrentChapter
import org.koin.compose.koinInject

@Composable
fun BottomLeftPlayerControls(
  playbackSpeed: Float,
  currentChapter: Segment?,
  onLockControls: () -> Unit,
  onCycleRotation: () -> Unit,
  onPlaybackSpeedChange: (Float) -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  modifier: Modifier = Modifier
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ControlsButton(
      Icons.Default.LockOpen,
      onClick = onLockControls,
    )
    ControlsButton(
      icon = Icons.Default.ScreenRotation,
      onClick = onCycleRotation
    )
    ControlsButton(
      text = stringResource(R.string.player_speed, playbackSpeed),
      onClick = {
        val newSpeed = if (playbackSpeed >= 2) 0.25f else playbackSpeed + 0.25f
        onPlaybackSpeedChange(newSpeed)
        playerPreferences.defaultSpeed.set(newSpeed)
      },
      onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
    )
    AnimatedVisibility(
      currentChapter != null && playerPreferences.currentChaptersIndicator.get(),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      CurrentChapter(
        chapter = currentChapter!!,
        onClick = { onOpenSheet(Sheets.Chapters) }
      )
    }
  }
}
