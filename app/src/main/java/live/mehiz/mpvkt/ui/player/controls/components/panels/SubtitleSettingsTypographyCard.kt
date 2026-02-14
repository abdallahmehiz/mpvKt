package live.mehiz.mpvkt.ui.player.controls.components.panels

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.k1rakishou.fsaf.FileManager
import com.yubyf.truetypeparser.TTFFile
import `is`.xyz.mpv.MPV
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitleJustification
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.presentation.components.ExpandableCard
import live.mehiz.mpvkt.presentation.components.ExposedTextDropDownMenu
import live.mehiz.mpvkt.presentation.components.SliderItem
import live.mehiz.mpvkt.ui.player.controls.CARDS_MAX_WIDTH
import live.mehiz.mpvkt.ui.player.controls.panelCardsColors
import live.mehiz.mpvkt.ui.theme.spacing
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceTheme
import org.koin.compose.koinInject

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SubtitleSettingsTypographyCard(
  isBold: Boolean,
  isItalic: Boolean,
  justify: SubtitleJustification,
  font: String,
  fontSize: Int,
  borderStyle: SubtitlesBorderStyle,
  borderSize: Int,
  shadowOffset: Int,
  onIsBoldChange: (Boolean) -> Unit,
  onIsItalicChange: (Boolean) -> Unit,
  onJustificationChange: (SubtitleJustification) -> Unit,
  onFontChange: (String) -> Unit,
  onFontSizeChange: (Int) -> Unit,
  onBorderStyleChange: (SubtitlesBorderStyle) -> Unit,
  onBorderSizeChange: (Int) -> Unit,
  onShadowOffsetChange: (Int) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val preferences = koinInject<SubtitlesPreferences>()
  val fileManager = koinInject<FileManager>()
  var isExpanded by remember { mutableStateOf(true) }
  val fonts by remember { mutableStateOf(mutableListOf(preferences.font.defaultValue())) }
  var fontsLoadingIndicator: (@Composable () -> Unit)? by remember {
    val indicator: (@Composable () -> Unit) = {
      CircularProgressIndicator(Modifier.size(32.dp))
    }
    mutableStateOf(indicator)
  }
  LaunchedEffect(Unit) {
    if (!preferences.fontsFolder.isSet()) {
      fontsLoadingIndicator = null
      return@LaunchedEffect
    }
    withContext(Dispatchers.IO) {
      fonts.addAll(
        fileManager.listFiles(
          fileManager.fromUri(preferences.fontsFolder.get().toUri()) ?: return@withContext,
        ).filter {
          fileManager.isFile(it) && fileManager.getName(it).lowercase().matches(".*\\.[ot]tf$".toRegex())
        }.mapNotNull {
          runCatching { TTFFile.open(fileManager.getInputStream(it)!!).families.values.first() }.getOrNull()
        }.distinct(),
      )
      fontsLoadingIndicator = null
    }
  }

  ExpandableCard(
    isExpanded = isExpanded,
    onExpand = { isExpanded = !isExpanded },
    title = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
      ) {
        Icon(Icons.Default.FormatColorText, null)
        Text(stringResource(R.string.player_sheets_sub_typography_card_title))
      }
    },
    modifier = modifier.widthIn(max = CARDS_MAX_WIDTH),
    colors = panelCardsColors(),
  ) {
    Column {
      Row(
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = MaterialTheme.spacing.extraSmall, end = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconToggleButton(
          checked = isBold,
          onCheckedChange = onIsBoldChange
        ) {
          Icon(
            Icons.Default.FormatBold,
            null,
            modifier = Modifier.size(32.dp),
          )
        }
        IconToggleButton(
          checked = isItalic,
          onCheckedChange = onIsItalicChange
        ) {
          Icon(
            Icons.Default.FormatItalic,
            null,
            modifier = Modifier.size(32.dp),
          )
        }
        SubtitleJustification.entries.minus(SubtitleJustification.Auto).forEach { justification ->
          IconToggleButton(
            checked = justify == justification,
            onCheckedChange = {
              onJustificationChange(justification)
              /*
              MPVLib.setPropertyBoolean("sub-ass-justify", it)
              if (it) {
                preferences.justification.set(justification)
                MPVLib.setPropertyString("sub-justify", justification.value)
              } else {
                preferences.justification.set(SubtitleJustification.Auto)
                MPVLib.setPropertyString("sub-justify", SubtitleJustification.Auto.value)
              }
               */
            },
          ) {
            Icon(justification.icon, null)
          }
        }
        Spacer(Modifier.weight(1f))
        TextButton(
          onClick = onReset,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.FormatClear, null)
            Text(stringResource(R.string.generic_reset))
          }
        }
      }
      Row(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painterResource(R.drawable.outline_brand_family_24),
          null,
          modifier = Modifier.size(32.dp),
        )
        ExposedTextDropDownMenu(
          selectedValue = font,
          options = fonts.toImmutableList(),
          label = stringResource(R.string.player_sheets_sub_typography_font),
          onValueChangedEvent = onFontChange,
          leadingIcon = fontsLoadingIndicator,
        )
      }
      SliderItem(
        label = stringResource(R.string.player_sheets_sub_typography_font_size),
        max = 100,
        min = 1,
        value = fontSize,
        valueText = fontSize.toString(),
        onChange = onFontSizeChange,
      ) {
        Icon(Icons.Default.FormatSize, null)
      }
      ProvidePreferenceLocals(
        theme = preferenceTheme(iconContainerMinWidth = 64.dp),
      ) {
        ListPreference(
          borderStyle,
          onValueChange = onBorderStyleChange,
          title = { Text(stringResource(R.string.player_sheets_subtitles_border_style)) },
          valueToText = { AnnotatedString(context.getString(it.titleRes)) },
          values = SubtitlesBorderStyle.entries,
          type = ListPreferenceType.DROPDOWN_MENU,
          summary = { Text(stringResource(borderStyle.titleRes)) },
          icon = { Icon(Icons.Default.BorderStyle, null) },
        )
      }
      SliderItem(
        stringResource(R.string.player_sheets_sub_typography_border_size),
        value = borderSize,
        valueText = borderSize.toString(),
        onChange = onBorderSizeChange,
        max = 100,
        icon = { Icon(Icons.Default.BorderColor, null) },
      )
      SliderItem(
        stringResource(R.string.player_sheets_subtitles_shadow_offset),
        value = shadowOffset,
        valueText = shadowOffset.toString(),
        onChange = onShadowOffsetChange,
        max = 100,
        icon = { Icon(painterResource(R.drawable.sharp_shadow_24), null) },
      )
    }
  }
}

fun resetTypography(
  mpv: MPV,
  preferences: SubtitlesPreferences
) {
  mpv.setPropertyBoolean("sub-bold", preferences.bold.deleteAndGet())
  mpv.setPropertyBoolean("sub-italic", preferences.italic.deleteAndGet())
  mpv.setPropertyBoolean("sub-ass-justify", preferences.overrideAssSubs.deleteAndGet())
  mpv.setPropertyString("sub-justify", preferences.justification.deleteAndGet().value)
  mpv.setPropertyString("sub-font", preferences.font.deleteAndGet())
  mpv.setPropertyInt("sub-font-size", preferences.fontSize.deleteAndGet())
  mpv.setPropertyInt("sub-border-size", preferences.borderSize.deleteAndGet())
  mpv.setPropertyInt("sub-shadow-offset", preferences.shadowOffset.deleteAndGet())
  mpv.setPropertyString("sub-border-style", preferences.borderStyle.deleteAndGet().value)
}

enum class SubtitlesBorderStyle(
  val value: String,
  @StringRes val titleRes: Int,
) {
  OutlineAndShadow("outline-and-shadow", R.string.player_sheets_subtitles_border_style_outline_and_shadow),
  OpaqueBox("opaque-box", R.string.player_sheets_subtitles_border_style_opaque_box),
  BackgroundBox("background-box", R.string.player_sheets_subtitles_border_style_background_box)
  ;

  companion object {
    fun byValue(value: String): SubtitlesBorderStyle {
      return when (value) {
        "outline-and-shadow" -> SubtitlesBorderStyle.OutlineAndShadow
        "opaque-box" -> SubtitlesBorderStyle.OpaqueBox
        "background-box" -> SubtitlesBorderStyle.BackgroundBox
        else -> throw IllegalArgumentException("This border style is unsupported")
      }
    }
  }
}
