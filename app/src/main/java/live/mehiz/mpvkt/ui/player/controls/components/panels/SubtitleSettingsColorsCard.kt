package live.mehiz.mpvkt.ui.player.controls.components.panels

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.Preference
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.presentation.components.ExpandableCard
import live.mehiz.mpvkt.presentation.components.TintedSliderItem
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject

@Composable
fun SubtitleSettingsColorsCard(
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<SubtitlesPreferences>()
  var isExpanded by remember { mutableStateOf(true) }
  ExpandableCard(
    isExpanded = isExpanded,
    onExpand = { isExpanded = !isExpanded },
    title = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        Icon(Icons.Default.Palette, null)
        Text(stringResource(R.string.player_sheets_sub_colors_card_title))
      }
    },
    modifier = modifier.widthIn(max = CARDS_MAX_WIDTH),
    colors = SubtitleSettingsCardColors(),
  ) {
    Column {
      var currentColorType by remember { mutableStateOf(SubColorType.Text) }
      var currentColor by remember { mutableIntStateOf(preferences.textColor.get()) }
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = MaterialTheme.spacing.extraSmall, end = MaterialTheme.spacing.medium),
      ) {
        SubColorType.entries.forEach { type ->
          IconToggleButton(
            checked = currentColorType == type,
            onCheckedChange = {
              currentColorType = type
              currentColor = when (currentColorType) {
                SubColorType.Text -> preferences.textColor.get()
                SubColorType.Border -> preferences.borderColor.get()
                SubColorType.Background -> preferences.backgroundColor.get()
              }
            },
          ) {
            Icon(
              when (type) {
                SubColorType.Text -> Icons.Default.FormatColorText
                SubColorType.Border -> Icons.Default.BorderColor
                SubColorType.Background -> Icons.Default.FormatColorFill
              },
              null,
            )
          }
        }
        Text(stringResource(currentColorType.titleRes))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { resetColors(preferences) }) {
          Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Icon(Icons.Default.FormatColorReset, null)
            Text(stringResource(R.string.generic_reset))
          }
        }
      }
      SubtitlesColorPicker(currentColorType)
    }
  }
}

fun Int.copyAsArgb(
  alpha: Int = this.alpha,
  red: Int = this.red,
  green: Int = this.green,
  blue: Int = this.blue,
) = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

@OptIn(ExperimentalStdlibApi::class)
fun Int.toColorHexString() = "#" + this.toHexString().uppercase()

enum class SubColorType(
  @StringRes val titleRes: Int,
  val property: String,
  val preference: (SubtitlesPreferences) -> Preference<Int>,
) {
  Text(
    R.string.player_sheets_subtitles_color_text,
    "sub-color",
    preference = SubtitlesPreferences::textColor,
  ),
  Border(
    R.string.player_sheets_subtitles_color_border,
    "sub-border-color",
    preference = SubtitlesPreferences::borderColor,
  ),
  Background(
    R.string.player_sheets_subtitles_color_background,
    "sub-back-color",
    preference = SubtitlesPreferences::backgroundColor,
  )
}

fun resetColors(preferences: SubtitlesPreferences) {
  MPVLib.setPropertyString("sub-color", preferences.textColor.deleteAndGet().toColorHexString())
  MPVLib.setPropertyString("sub-border-color", preferences.borderColor.deleteAndGet().toColorHexString())
  MPVLib.setPropertyString("sub-back-color", preferences.backgroundColor.deleteAndGet().toColorHexString())
}

@Composable
fun SubtitlesColorPicker(type: SubColorType) {
  val preferences = koinInject<SubtitlesPreferences>()
  val preference = type.preference(preferences)
  val currentColor by preference.collectAsState()
  LaunchedEffect(currentColor) {
    MPVLib.setPropertyString(type.property, currentColor.toColorHexString())
  }
  TintedSliderItem(
    stringResource(R.string.player_sheets_sub_color_red),
    currentColor.red,
    currentColor.red.toString(),
    onChange = { preference.set(currentColor.copyAsArgb(red = it)) },
    max = 255,
    tint = Color.Red,
  )

  TintedSliderItem(
    stringResource(R.string.player_sheets_sub_color_green),
    currentColor.green,
    currentColor.green.toString(),
    onChange = { preference.set(currentColor.copyAsArgb(green = it)) },
    max = 255,
    tint = Color.Green,
  )

  TintedSliderItem(
    stringResource(R.string.player_sheets_sub_color_blue),
    currentColor.blue,
    currentColor.blue.toString(),
    onChange = { preference.set(currentColor.copyAsArgb(blue = it)) },
    max = 255,
    tint = Color.Blue,
  )

  TintedSliderItem(
    stringResource(R.string.player_sheets_sub_color_alpha),
    currentColor.alpha,
    currentColor.alpha.toString(),
    onChange = { preference.set(currentColor.copyAsArgb(alpha = it)) },
    max = 255,
    tint = Color.White,
  )
}
