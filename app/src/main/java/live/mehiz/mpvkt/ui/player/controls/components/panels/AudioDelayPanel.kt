package live.mehiz.mpvkt.ui.player.controls.components.panels

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject

@Composable
fun AudioDelayPanel(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<AudioPreferences>()

  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(MaterialTheme.spacing.medium),
  ) {
    val delayControlCard = createRef()

    val delay by MPVLib.propDouble["audio-delay"].collectAsState()
    DelayCard(
      delayMs = (delay!! * 1000).toInt(),
      onDelayChange = { MPVLib.setPropertyDouble("audio-delay", it / 1000.0) },
      onApply = { preferences.defaultAudioDelay.set((delay!! * 1000).toInt()) },
      onReset = { MPVLib.setPropertyDouble("audio-delay", (preferences.defaultAudioDelay.get() / 1000.0)) },
      title = { AudioDelayCardTitle(onClose = onDismissRequest) },
      delayType = DelayType.Audio,
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
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      stringResource(R.string.player_sheets_audio_delay_card_title),
      style = MaterialTheme.typography.headlineMedium,
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
