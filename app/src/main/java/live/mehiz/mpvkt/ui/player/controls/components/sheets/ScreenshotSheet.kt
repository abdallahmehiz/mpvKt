package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.ui.player.ScreenshotFormat
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Composable
fun ScreenshotSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier = modifier
  ) {
    ProvidePreferenceLocals {
      Column {
        val withSubtitles by playerPreferences.takeScreenshotsWithSubtitles.collectAsState()
        SwitchPreference(
          value = withSubtitles,
          onValueChange = playerPreferences.takeScreenshotsWithSubtitles::set,
          title = { Text(stringResource(R.string.player_sheets_screenshot_include_subs)) }
        )
        val format by playerPreferences.screenshotsFileFormat.collectAsState()
        ListPreference(
          value = format,
          onValueChange = {
            playerPreferences.screenshotsFileFormat.set(it)
            MPVLib.setPropertyString("screenshot-format", it.propertyValue)
          },
          title = { Text(stringResource(R.string.player_sheets_screenshot_file_format)) },
          summary = { Text(format.displayName) },
          valueToText = { AnnotatedString(it.displayName) },
          values = ScreenshotFormat.entries
        )
      }
    }
  }
}
