package live.mehiz.mpvkt.ui.player.controls.components.panels

import android.annotation.SuppressLint
import android.net.Uri
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
import com.github.k1rakishou.fsaf.FileManager
import com.yubyf.truetypeparser.TTFFile
import `is`.xyz.mpv.MPVLib
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
          fileManager.fromUri(Uri.parse(preferences.fontsFolder.get())) ?: return@withContext,
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
      var isBold by remember { mutableStateOf(MPVLib.getPropertyBoolean("sub-bold")) }
      var isItalic by remember { mutableStateOf(MPVLib.getPropertyBoolean("sub-italic")) }
      var justify by remember {
        mutableStateOf(
          SubtitleJustification.entries.first {
            it.value == MPVLib.getPropertyString("sub-justify")
          },
        )
      }
      var font by remember { mutableStateOf(MPVLib.getPropertyString("sub-font")) }
      var fontSize by remember {
        mutableStateOf(MPVLib.getPropertyInt("sub-font-size"))
      }
      var borderStyle by remember {
        mutableStateOf(
          SubtitlesBorderStyle.entries.first { it.value == MPVLib.getPropertyString("sub-border-style") }
        )
      }
      var borderSize by remember {
        mutableStateOf(
          MPVLib.getPropertyInt("sub-border-size")
        )
      }
      var shadowOffset by remember {
        mutableStateOf(
          MPVLib.getPropertyInt("sub-shadow-offset")
        )
      }
      Row(
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = MaterialTheme.spacing.extraSmall, end = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconToggleButton(
          checked = isBold,
          onCheckedChange = {
            isBold = it
            preferences.bold.set(it)
            MPVLib.setPropertyBoolean("sub-bold", it)
          },
        ) {
          Icon(
            Icons.Default.FormatBold,
            null,
            modifier = Modifier.size(32.dp),
          )
        }
        IconToggleButton(
          checked = isItalic,
          onCheckedChange = {
            isItalic = it
            preferences.italic.set(it)
            MPVLib.setPropertyBoolean("sub-italic", it)
          },
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
              justify = justification
              MPVLib.setPropertyBoolean("sub-ass-justify", it)
              if (it) {
                preferences.justification.set(justification)
                MPVLib.setPropertyString("sub-justify", justification.value)
              } else {
                preferences.justification.set(SubtitleJustification.Auto)
                MPVLib.setPropertyString("sub-justify", SubtitleJustification.Auto.value)
              }
            },
          ) {
            Icon(justification.icon, null)
          }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = {
          resetTypography(preferences)
          isBold = MPVLib.getPropertyBoolean("sub-bold")
          isItalic = MPVLib.getPropertyBoolean("sub-italic")
          justify = SubtitleJustification.entries.first { it.value == MPVLib.getPropertyString("sub-justify") }
          font = MPVLib.getPropertyString("sub-font")
          fontSize = MPVLib.getPropertyInt("sub-font-size")
          borderStyle = SubtitlesBorderStyle.entries.first { it.value == MPVLib.getPropertyString("sub-border-style") }
          borderSize = MPVLib.getPropertyInt("sub-border-size")
          shadowOffset = MPVLib.getPropertyInt("sub-shadow-offset")
        }) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
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
          onValueChangedEvent = {
            font = it
            preferences.font.set(it)
            MPVLib.setPropertyString("sub-font", it)
          },
          leadingIcon = fontsLoadingIndicator,
        )
      }
      SliderItem(
        label = stringResource(R.string.player_sheets_sub_typography_font_size),
        max = 100,
        min = 1,
        value = fontSize,
        valueText = fontSize.toString(),
        onChange = {
          fontSize = it
          preferences.fontSize.set(it)
          MPVLib.setPropertyInt("sub-font-size", it)
        },
      ) {
        Icon(Icons.Default.FormatSize, null)
      }
      ProvidePreferenceLocals(
        theme = preferenceTheme(iconContainerMinWidth = 64.dp),
      ) {
        ListPreference(
          borderStyle,
          onValueChange = {
            borderStyle = it
            preferences.borderStyle.set(it)
            MPVLib.setPropertyString("sub-border-style", it.value)
          },
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
        onChange = {
          borderSize = it
          preferences.borderSize.set(it)
          MPVLib.setPropertyInt("sub-border-size", it)
        },
        max = 100,
        icon = { Icon(Icons.Default.BorderColor, null) },
      )
      SliderItem(
        stringResource(R.string.player_sheets_subtitles_shadow_offset),
        value = shadowOffset,
        valueText = shadowOffset.toString(),
        onChange = {
          shadowOffset = it
          preferences.shadowOffset.set(it)
          MPVLib.setPropertyInt("sub-shadow-offset", it)
        },
        max = 100,
        icon = { Icon(painterResource(R.drawable.sharp_shadow_24), null) },
      )
    }
  }
}

fun resetTypography(preferences: SubtitlesPreferences) {
  MPVLib.setPropertyBoolean("sub-bold", preferences.bold.deleteAndGet())
  MPVLib.setPropertyBoolean("sub-italic", preferences.italic.deleteAndGet())
  MPVLib.setPropertyBoolean("sub-ass-justify", preferences.overrideAssSubs.deleteAndGet())
  MPVLib.setPropertyString("sub-justify", preferences.justification.deleteAndGet().value)
  MPVLib.setPropertyString("sub-font", preferences.font.deleteAndGet())
  MPVLib.setPropertyInt("sub-font-size", preferences.fontSize.deleteAndGet())
  MPVLib.setPropertyInt("sub-border-size", preferences.borderSize.deleteAndGet())
  MPVLib.setPropertyInt("sub-shadow-offset", preferences.shadowOffset.deleteAndGet())
  MPVLib.setPropertyString("sub-border-style", preferences.borderStyle.deleteAndGet().value)
}

enum class SubtitlesBorderStyle(
  val value: String,
  @StringRes val titleRes: Int,
) {
  OutlineAndShadow("outline-and-shadow", R.string.player_sheets_subtitles_border_style_outline_and_shadow),
  OpaqueBox("opaque-box", R.string.player_sheets_subtitles_border_style_opaque_box),
  BackgroundBox("background-box", R.string.player_sheets_subtitles_border_style_background_box)
}
