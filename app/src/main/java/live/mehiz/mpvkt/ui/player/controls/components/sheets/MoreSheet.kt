package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioChannels
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject

@Composable
fun MoreSheet(
  onDismissRequest: () -> Unit,
  onEnterFiltersPanel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(id = R.string.player_sheets_more_title),
          style = MaterialTheme.typography.headlineMedium,
        )
        TextButton(onClick = onEnterFiltersPanel) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(imageVector = Icons.Default.Tune, contentDescription = null)
            Text(text = stringResource(id = R.string.player_sheets_filters_title))
          }
        }
      }
      Text(stringResource(R.string.player_sheets_stats_page_title))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        items(4) { page ->
          FilterChip(
            label = {
              Text(
                stringResource(
                  if (page == 0) R.string.player_sheets_tracks_off else R.string.player_sheets_stats_page_chip,
                  page,
                ),
              )
            },
            onClick = {
              if ((page == 0) xor (statisticsPage == 0)) {
                MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
              }
              if (page != 0) {
                MPVLib.command(arrayOf("script-binding", "stats/display-page-$page"))
              }
              advancedPreferences.enabledStatisticsPage.set(page)
            },
            selected = statisticsPage == page,
          )
        }
      }
      Text(text = stringResource(id = R.string.pref_audio_channels))
      val audioChannels by audioPreferences.audioChannels.collectAsState()
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        items(AudioChannels.entries) {
          FilterChip(
            selected = audioChannels == it,
            onClick = {
              audioPreferences.audioChannels.set(it)
              if (it == AudioChannels.ReverseStereo) {
                MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
              } else {
                MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
              }
              MPVLib.setPropertyString(it.property, it.value)
            },
            label = { Text(text = stringResource(id = it.title)) },
          )
        }
      }
    }
  }
}
