package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackSpeedSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<PlayerPreferences>()
  val viewModel = koinInject<PlayerViewModel>()
  val currentSpeed by viewModel.playbackSpeed.collectAsState()
  LaunchedEffect(currentSpeed) {
    MPVLib.setPropertyDouble("speed", currentSpeed.toDouble())
  }
  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = MaterialTheme.spacing.medium),
    ) {
      SliderItem(
        label = stringResource(id = R.string.player_sheets_speed_slider_label),
        value = currentSpeed,
        valueText = stringResource(id = R.string.player_speed, currentSpeed),
        onChange = { newSpeed ->
          viewModel.playbackSpeed.update { newSpeed.toFixed(2) }
        },
        max = 6f,
        min = 0.01f,
      )
      val playbackSpeedPresets by preferences.speedPresets.collectAsState()
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        FilledTonalIconButton(onClick = {
          preferences.speedPresets.delete()
        }) {
          Icon(Icons.Default.RestartAlt, null)
        }
        LazyRow(
          modifier = Modifier
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
          items(
            playbackSpeedPresets.map { it.toFloat() }.sorted(),
          ) {
            InputChip(
              selected = currentSpeed == it,
              onClick = { viewModel.playbackSpeed.update { _ -> it } },
              label = { Text(stringResource(R.string.player_speed, it)) },
              modifier = Modifier
                .animateItemPlacement(),
              trailingIcon = {
                Icon(
                  Icons.Default.Close,
                  null,
                  modifier = Modifier
                    .clickable { preferences.speedPresets.set(playbackSpeedPresets.minus(it.toString())) },
                )
              },
            )
          }
        }
        FilledTonalIconButton(
          onClick = {
            preferences.speedPresets.set(playbackSpeedPresets.plus(currentSpeed.toString()))
          },
        ) {
          Icon(Icons.Default.Add, null)
        }
      }
      ProvidePreferenceLocals {
        val audioPreferences = koinInject<AudioPreferences>()
        val pitchCorrection by audioPreferences.audioPitchCorrection.collectAsState()
        SwitchPreference(
          value = pitchCorrection,
          onValueChange = {
            audioPreferences.audioPitchCorrection.set(it)
            MPVLib.setPropertyBoolean("audio-pitch-correction", it)
          },
          title = { Text(text = stringResource(id = R.string.pref_audio_pitch_correction_title)) },
          summary = { Text(text = stringResource(id = R.string.pref_audio_pitch_correction_summary)) },
        )
      }
      Row(
        modifier = Modifier
          .padding(horizontal = MaterialTheme.spacing.medium),
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

fun Float.toFixed(precision: Int = 1): Float {
  val factor = 10.0f.pow(precision)
  return (this * factor).roundToInt() / factor
}
