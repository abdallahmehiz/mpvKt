package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import org.koin.compose.koinInject

@Composable
fun TopRightPlayerControls(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier
) {
  Row(modifier) {
    val currentDecoder by viewModel.currentDecoder.collectAsState()
    val playerPreferences = koinInject<PlayerPreferences>()
    ControlsButton(
      currentDecoder.title,
      onClick = { viewModel.cycleDecoders() },
      onLongClick = { viewModel.sheetShown.update { Sheets.Decoders } },
    )
    if (playerPreferences.showChaptersButton.get() && viewModel.chapters.isNotEmpty()) {
      ControlsButton(
        Icons.Default.Bookmarks,
        onClick = { viewModel.sheetShown.update { Sheets.Chapters } },
      )
    }
    ControlsButton(
      Icons.Default.Subtitles,
      onClick = { viewModel.sheetShown.update { Sheets.SubtitlesSheet } },
    )
    ControlsButton(
      Icons.Default.Audiotrack,
      onClick = { viewModel.sheetShown.update { Sheets.AudioSheet } },
    )
    ControlsButton(
      Icons.Default.MoreVert,
      onClick = { viewModel.sheetShown.update { Sheets.More } },
    )
  }
}
