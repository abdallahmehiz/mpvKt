package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.sheets.subtitles.DelayCard
import org.koin.compose.koinInject

@Composable
fun AudioDelaySheet(
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<AudioPreferences>()
  val viewModel = koinInject<PlayerViewModel>()

  BackHandler { viewModel.sheetShown.update { Sheets.None } }

  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
  ) {
    val delayControlCard = createRef()

    var delay by remember { mutableIntStateOf((MPVLib.getPropertyDouble("audio-delay") * 1000).toInt()) }
    LaunchedEffect(delay) {
      MPVLib.setPropertyDouble("audio-delay", delay / 1000.0)
    }
    DelayCard(
      delay = delay,
      onDelayChange = { delay = it },
      onApply = { preferences.defaultAudioDelay.set(delay) },
      onReset = { delay = 0 },
      title = { AudioDelayCardTitle(onClose = { viewModel.sheetShown.update { Sheets.None } }) },
      modifier = Modifier.constrainAs(delayControlCard) {
        linkTo(parent.top, parent.bottom, bias = 0.8f)
        end.linkTo(parent.end)
      },
    )
  }
}

@Composable
fun AudioDelayCardTitle(
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      stringResource(R.string.player_sheets_audio_delay_card_title),
      style = MaterialTheme.typography.headlineMedium
    )
    IconButton(onClose) {
      Icon(
        Icons.Default.Close,
        null,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}
