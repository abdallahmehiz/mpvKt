package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import org.koin.compose.koinInject

@Composable
fun MoreSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  PlayerSheet(
    onDismissRequest,
    modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Text(stringResource(R.string.player_sheets_stats_page_title))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(4) { page ->
          FilterChip(
            label = {
              Text(
                stringResource(
                  if (page == 0) {
                    R.string.player_sheets_tracks_off
                  } else {
                    R.string.player_sheets_stats_page_chip
                  },
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
    }
  }
}
