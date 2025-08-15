package live.mehiz.mpvkt.ui.player.controls.components.panels

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.presentation.components.OutlinedNumericChooser
import live.mehiz.mpvkt.ui.player.controls.CARDS_MAX_WIDTH
import live.mehiz.mpvkt.ui.player.controls.panelCardsColors
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import kotlin.math.round

@Composable
fun SubtitleDelayPanel(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<SubtitlesPreferences>()

  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(MaterialTheme.spacing.medium),
  ) {
    val delayControlCard = createRef()

    var affectedSubtitle by remember { mutableStateOf(SubtitleDelayType.Primary) }
    val delay by MPVLib.propDouble["sub-delay"].collectAsState()
    val delayInt by remember { derivedStateOf { (delay!! * 1000).toInt() } }
    val secondaryDelay by MPVLib.propDouble["secondary-sub-delay"].collectAsState()
    val secondaryDelayInt by remember { derivedStateOf { (secondaryDelay!! * 1000).toInt() } }
    val speed by MPVLib.propFloat["sub-speed"].collectAsState()
    SubtitleDelayCard(
      delayMs = if (affectedSubtitle == SubtitleDelayType.Secondary) secondaryDelayInt else delayInt,
      onDelayChange = {
        when (affectedSubtitle) {
          SubtitleDelayType.Both -> {
            MPVLib.setPropertyFloat("sub-delay", it / 1000f)
            MPVLib.setPropertyFloat("secondary-sub-delay", it / 1000f)
          }

          SubtitleDelayType.Primary -> MPVLib.setPropertyFloat("sub-delay", it / 1000f)
          else -> MPVLib.setPropertyFloat("secondary-sub-delay", it / 1000f)
        }
      },
      speed = speed!!,
      onSpeedChange = { MPVLib.setPropertyFloat("sub-speed", round(it * 1000) / 1000f) },
      affectedSubtitle = affectedSubtitle,
      onTypeChange = { affectedSubtitle = it },
      onApply = {
        preferences.defaultSubDelay.set(delayInt)
        if (speed!! in 0.1f..10f) preferences.defaultSubSpeed.set(speed!!)
      },
      onReset = {
        MPVLib.setPropertyFloat("sub-delay", preferences.defaultSubDelay.get() / 1000f)
        MPVLib.setPropertyFloat("secondary-sub-delay", preferences.defaultSecondarySubDelay.get() / 1000f)
        MPVLib.setPropertyFloat("sub-speed", preferences.defaultSubSpeed.get())
      },
      onClose = onDismissRequest,
      modifier = Modifier.constrainAs(delayControlCard) {
        linkTo(parent.top, parent.bottom, bias = 0.8f)
        end.linkTo(parent.end)
      },
    )
  }
}

@Composable
fun SubtitleDelayCard(
  delayMs: Int,
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
    delayMs = delayMs,
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
            step = .01f,
            min = .1f,
          )
        }

        else -> {}
      }
    },
    delayType = DelayType.Subtitle,
    modifier = modifier,
  )
}

enum class SubtitleDelayType(
  @StringRes val title: Int,
) {
  Primary(R.string.player_sheets_sub_delay_subtitle_type_primary),
  Secondary(R.string.player_sheets_sub_delay_subtitle_type_secondary),
  Both(R.string.player_sheets_sub_delay_subtitle_type_primary_and_secondary),
}

@Suppress("LambdaParameterInRestartableEffect") // Intentional
@Composable
fun DelayCard(
  delayMs: Int,
  onDelayChange: (Int) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  title: @Composable () -> Unit,
  delayType: DelayType,
  modifier: Modifier = Modifier,
  extraSettings: @Composable ColumnScope.() -> Unit = {},
) {
  Card(
    modifier = modifier
      .widthIn(max = CARDS_MAX_WIDTH)
      .animateContentSize(),
    colors = panelCardsColors(),
  ) {
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.smaller,
        ),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      title()
      OutlinedNumericChooser(
        label = { Text(stringResource(R.string.player_sheets_sub_delay_card_delay)) },
        value = delayMs,
        onChange = onDelayChange,
        step = 50,
        min = Int.MIN_VALUE,
        max = Int.MAX_VALUE,
        suffix = { Text(stringResource(R.string.generic_unit_ms)) },
      )
      Column(
        modifier = Modifier.animateContentSize(),
      ) { extraSettings() }
      // true (heard -> spotted), false (spotted -> heard)
      var isDirectionPositive by remember { mutableStateOf<Boolean?>(null) }
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        var timerStart by remember { mutableStateOf<Long?>(null) }
        var finalDelay by remember { mutableIntStateOf(delayMs) }
        LaunchedEffect(isDirectionPositive) {
          if (isDirectionPositive == null) {
            onDelayChange(finalDelay)
            return@LaunchedEffect
          }
          finalDelay = delayMs
          timerStart = System.currentTimeMillis()
          val startingDelay: Int = finalDelay
          while (isDirectionPositive != null && timerStart != null) {
            val elapsed = System.currentTimeMillis() - timerStart!!
            finalDelay = startingDelay + (if (isDirectionPositive!!) elapsed else -elapsed).toInt()
            // Arbitrary delay of 20ms
            delay(20)
          }
        }
        Button(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) delayType == DelayType.Audio else null
          },
          modifier = Modifier.weight(1f),
          enabled = isDirectionPositive != (delayType == DelayType.Audio),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_audio_sound_heard
              } else {
                R.string.player_sheets_sub_delay_subtitle_voice_heard
              },
            ),
          )
        }
        Button(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) delayType != DelayType.Audio else null
          },
          modifier = Modifier.weight(1f),
          enabled = isDirectionPositive != (delayType == DelayType.Subtitle),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_sound_sound_spotted
              } else {
                R.string.player_sheets_sub_delay_subtitle_text_seen
              },
            ),
          )
        }
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        Button(
          onClick = onApply,
          modifier = Modifier.weight(1f),
          enabled = isDirectionPositive == null,
        ) {
          Text(stringResource(R.string.player_sheets_delay_set_as_default))
        }
        FilledIconButton(
          onClick = onReset,
          enabled = isDirectionPositive == null,
        ) {
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
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      stringResource(R.string.player_sheets_sub_delay_card_title),
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

enum class DelayType {
  Audio, Subtitle
}
