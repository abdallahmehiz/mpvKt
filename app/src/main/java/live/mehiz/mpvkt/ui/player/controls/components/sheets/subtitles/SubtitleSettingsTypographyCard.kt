package live.mehiz.mpvkt.ui.player.controls.components.sheets.subtitles

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.presentation.components.ExpandableCard
import live.mehiz.mpvkt.presentation.components.ExposedTextDropDownMenu
import live.mehiz.mpvkt.presentation.components.SliderItem
import org.koin.compose.koinInject

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SubtitleSettingsTypographyCard(
  modifier: Modifier = Modifier,
) {
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
        }.map { TTFFile.open(fileManager.getInputStream(it)!!).families.values.first() },
      )
      fontsLoadingIndicator = null
    }
  }

  ExpandableCard(
    isExpanded = isExpanded,
    onExpand = { isExpanded = !isExpanded },
    title = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Icon(Icons.Default.FormatColorText, null)
        Text(stringResource(R.string.player_sheets_sub_typography_card_title))
      }
    },
    modifier = modifier.widthIn(max = CARDS_MAX_WIDTH),
    colors = SubtitleSettingsCardColors(),
  ) {
    Column {
      val isBold by preferences.bold.collectAsState()
      val isItalic by preferences.italic.collectAsState()
      val justify by preferences.justification.collectAsState()
      Row(
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconToggleButton(
          checked = isBold,
          onCheckedChange = {
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
            checked = justify.value == justification.value,
            onCheckedChange = {
              if (it) {
                preferences.justification.set(justification)
              } else {
                preferences.justification.set(SubtitleJustification.Auto)
              }
              MPVLib.setPropertyBoolean("sub-ass-justify", preferences.overrideAssSubs.get())
              MPVLib.setPropertyString("sub-justify", preferences.justification.get().value)
            },
          ) {
            Icon(justification.icon, null)
          }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { resetTypography(preferences) }) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.FormatClear, null)
            Text(stringResource(R.string.generic_reset))
          }
        }
      }
      val font by preferences.font.collectAsState()
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            preferences.font.set(it)
            MPVLib.setPropertyString("sub-font", it)
          },
          leadingIcon = fontsLoadingIndicator,
        )
      }
      val size by preferences.fontSize.collectAsState()
      SliderItem(
        label = stringResource(R.string.player_sheets_sub_typography_font_size),
        max = 100,
        min = 1,
        value = size,
        valueText = size.toString(),
        onChange = {
          preferences.fontSize.set(it)
          MPVLib.setPropertyInt("sub-font-size", it)
        },
      ) {
        Icon(Icons.Default.FormatSize, null)
      }
      val border by preferences.borderSize.collectAsState()
      SliderItem(
        label = stringResource(R.string.player_sheets_sub_typography_border_size),
        border,
        valueText = border.toString(),
        onChange = {
          preferences.borderSize.set(it)
          MPVLib.setPropertyInt("sub-border-size", it)
        },
        max = 30,
      ) { Icon(Icons.Default.BorderColor, null) }
    }
  }
}

fun resetTypography(preferences: SubtitlesPreferences) {
  MPVLib.setPropertyString("sub-font", preferences.font.deleteAndGet())
  MPVLib.setPropertyBoolean("sub-bold", preferences.bold.deleteAndGet())
  MPVLib.setPropertyBoolean("sub-italic", preferences.italic.deleteAndGet())
  MPVLib.setPropertyInt("sub-font-size", preferences.fontSize.deleteAndGet())
  MPVLib.setPropertyInt("sub-border-size", preferences.borderSize.deleteAndGet())
}
