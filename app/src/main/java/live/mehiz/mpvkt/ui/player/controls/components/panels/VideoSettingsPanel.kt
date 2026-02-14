package live.mehiz.mpvkt.ui.player.controls.components.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.ui.player.DebandSettings
import live.mehiz.mpvkt.ui.player.Debanding
import live.mehiz.mpvkt.ui.player.VideoFilters
import live.mehiz.mpvkt.ui.player.controls.components.panels.components.MultiCardPanel

@Composable
fun VideoSettingsPanel(
  onDismissRequest: () -> Unit,
  onVideoFilterChange: (VideoFilters, Int) -> Unit,
  // Deband settings
  deband: Debanding,
  onDebandChange: (Debanding) -> Unit,
  debandSettings: (DebandSettings) -> Int,
  onDebandSettingsChange: (DebandSettings, Int) -> Unit,
  onDebandReset: () -> Unit,
  // Filter settings
  isGpuNextEnabled: Boolean,
  filterValue: (VideoFilters) -> Int,
  onFilterReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  MultiCardPanel(
    onDismissRequest = onDismissRequest,
    titleRes = R.string.player_sheets_video_settings_title,
    cardCount = 2,
    modifier = modifier,
  ) { index, cardModifier ->
    when (index) {
      0 -> VideoSettingsDebandCard(
        deband = deband,
        onDebandingChange = onDebandChange,
        debandSettingsValue = debandSettings,
        onDebandingSettingsChange = onDebandSettingsChange,
        onReset = onDebandReset,
        modifier = cardModifier
      )
      1 -> VideoSettingsFiltersCard(
        isGpuNextEnabled = isGpuNextEnabled,
        filterValue = filterValue,
        onFilterValueChange = onVideoFilterChange,
        onReset = onFilterReset,
        modifier = cardModifier,
      )
      else -> {}
    }
  }
}
