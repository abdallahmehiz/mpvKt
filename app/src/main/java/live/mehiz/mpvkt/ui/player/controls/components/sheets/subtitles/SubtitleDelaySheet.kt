package live.mehiz.mpvkt.ui.player.controls.components.sheets.subtitles

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.presentation.components.OutlinedNumericChooser
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import org.koin.compose.koinInject
import kotlin.math.round

@Composable
fun SubtitleDelaySheet(
  modifier: Modifier = Modifier
) {
  val preferences = koinInject<SubtitlesPreferences>()
  val viewModel = koinInject<PlayerViewModel>()

  BackHandler { viewModel.sheetShown.update { Sheets.None } }
  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
  ) {
    val delayControlCard = createRef()

    var affectedSubtitle by remember { mutableStateOf(SubtitleDelayType.Primary) }
    var delay by remember { mutableIntStateOf((MPVLib.getPropertyDouble("sub-delay") * 1000).toInt()) }
    var speed by remember { mutableFloatStateOf(MPVLib.getPropertyDouble("sub-speed").toFloat()) }
    LaunchedEffect(speed) {
      MPVLib.setPropertyDouble("sub-speed", speed.toDouble())
    }
    LaunchedEffect(delay) {
      val finalDelay = delay / 1000.0
      when (affectedSubtitle) {
        SubtitleDelayType.Primary -> MPVLib.setPropertyDouble("sub-delay", finalDelay)
        SubtitleDelayType.Secondary -> MPVLib.setPropertyDouble("secondary-sub-delay", finalDelay)
        else -> {
          MPVLib.setPropertyDouble("sub-delay", finalDelay)
          MPVLib.setPropertyDouble("secondary-sub-delay", finalDelay)
        }
      }
    }
    SubtitleDelayCard(
      delay = delay,
      onDelayChange = { delay = it },
      speed = speed,
      onSpeedChange = { speed = round(it * 10) / 10f },
      affectedSubtitle = affectedSubtitle,
      onTypeChange = { affectedSubtitle = it },
      onApply = {
        preferences.defaultSubDelay.set(delay)
        preferences.defaultSubSpeed.set(speed)
      },
      onReset = {
        delay = 0
        speed = 1f
      },
      onClose = { viewModel.sheetShown.update { Sheets.None } },
      modifier = Modifier.constrainAs(delayControlCard) {
        linkTo(parent.top, parent.bottom, bias = 0.8f)
        end.linkTo(parent.end)
      },
    )
  }
}

@Composable
fun SubtitleDelayCard(
  delay: Int,
  onDelayChange: (Int) -> Unit,
  speed: Float,
  onSpeedChange: (Float) -> Unit,
  affectedSubtitle: SubtitleDelayType,
  onTypeChange: (SubtitleDelayType) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DelayCard(
    delay = delay,
    onDelayChange = onDelayChange,
    onApply = onApply,
    onReset = onReset,
    title = {
      SubtitleDelayTitle(
        affectedSubtitle = affectedSubtitle,
        onClose = onClose,
        onTypeChange = onTypeChange,
      )
    },
    extraSettings = {
      when (affectedSubtitle) {
        SubtitleDelayType.Primary -> {
          OutlinedNumericChooser(
            label = { Text(stringResource(R.string.player_sheets_sub_delay_card_speed)) },
            value = speed,
            onChange = onSpeedChange,
            max = 10f,
            step = .1f,
            min = .1f
          )
        }
        else -> {}
      }
    },
    modifier = modifier,
  )
}

enum class SubtitleDelayType(
  @StringRes val title: Int,
) {
  Primary(
    R.string.player_sheets_sub_delay_subtitle_type_primary
  ), Secondary(R.string.player_sheets_sub_delay_subtitle_type_secondary), Both(
    R.string.player_sheets_sub_delay_subtitle_type_primary_and_secondary,
  ),
}

@Composable
fun DelayCard(
  delay: Int,
  onDelayChange: (Int) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  extraSettings: @Composable ColumnScope.() -> Unit = {},
) {
  Card(
    modifier = modifier
      .widthIn(max = CARDS_MAX_WIDTH)
      .animateContentSize(),
    colors = SubtitleSettingsCardColors(),
  ) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      title()
      OutlinedNumericChooser(
        label = { Text(stringResource(R.string.player_sheets_sub_delay_card_delay)) },
        value = delay,
        onChange = onDelayChange,
        step = 50,
        min = Int.MIN_VALUE,
        max = Int.MAX_VALUE,
        suffix = { Text(stringResource(R.string.generic_unit_ms)) }
      )
      Column(
        modifier = Modifier.animateContentSize()
      ) { extraSettings() }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onApply,
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.player_sheets_delay_set_as_default))
        }
        FilledIconButton(onClick = onReset) {
          Icon(Icons.Default.Refresh, null)
        }
      }
    }
  }
}

@Composable
fun SubtitleDelayTitle(
  affectedSubtitle: SubtitleDelayType,
  onClose: () -> Unit,
  onTypeChange: (SubtitleDelayType) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      "Subtitle Delay",
      style = MaterialTheme.typography.headlineMedium,
    )
    var showDropDownMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.clickable { showDropDownMenu = true }) {
      Text(
        stringResource(affectedSubtitle.title),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      Icon(Icons.Default.ArrowDropDown, null)
      DropdownMenu(
        expanded = showDropDownMenu,
        onDismissRequest = { showDropDownMenu = false },
      ) {
        SubtitleDelayType.entries.forEach {
          DropdownMenuItem(
            text = { Text(stringResource(it.title)) },
            onClick = {
              onTypeChange(it)
              showDropDownMenu = false
            },
          )
        }
      }
    }
    Spacer(Modifier.weight(1f))
    IconButton(onClose) {
      Icon(
        Icons.Default.Close,
        null,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}
