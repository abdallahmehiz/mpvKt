package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.presentation.components.SliderItem
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.theme.spacing
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Composable
fun PlaybackSpeedSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<PlayerPreferences>()
  val viewModel = koinInject<PlayerViewModel>()
  val currentSpeed by viewModel.playbackSpeed.collectAsState()
  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier.padding(MaterialTheme.spacing.medium),
    ) {
      SliderItem(
        label = stringResource(id = R.string.player_sheets_speed_slider_label),
        value = currentSpeed,
        valueText = stringResource(id = R.string.player_speed, currentSpeed),
        onChange = { newSpeed ->
          viewModel.playbackSpeed.update { newSpeed }
          MPVLib.setPropertyDouble("speed", newSpeed.toDouble())
        },
        max = 6f,
        min = 0.01f,
      )
      ProvidePreferenceLocals {
        val audioPreferences = koinInject<AudioPreferences>()
        val pitchCorrection by audioPreferences.audioPitchCorrection.collectAsState()
        SwitchPreference(
          value = pitchCorrection,
          onValueChange = { audioPreferences.audioPitchCorrection.set(it) },
          title = { Text(text = stringResource(id = R.string.pref_audio_pitch_correction_title)) },
          summary = { Text(text = stringResource(id = R.string.pref_audio_pitch_correction_summary)) },
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        Button(
          modifier = Modifier.weight(1f),
          onClick = {
            preferences.defaultSpeed.set(currentSpeed)
          },
        ) {
          Text(text = stringResource(id = R.string.player_sheets_speed_make_default))
        }
        FilledIconButton(
          onClick = {
            preferences.defaultSpeed.delete()
            viewModel.playbackSpeed.update { 1f }
            MPVLib.setPropertyDouble("speed", 1.0)
          },
        ) {
          Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
        }
      }
    }
  }
}
