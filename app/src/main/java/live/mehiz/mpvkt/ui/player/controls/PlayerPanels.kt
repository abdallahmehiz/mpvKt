package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitleJustification
import live.mehiz.mpvkt.ui.player.DebandSettings
import live.mehiz.mpvkt.ui.player.Debanding
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.VideoFilters
import live.mehiz.mpvkt.ui.player.controls.components.panels.AudioDelayPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubColorType
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitleDelayPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitleSettingsPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitlesBorderStyle
import live.mehiz.mpvkt.ui.player.controls.components.panels.VideoSettingsPanel
import org.koin.compose.koinInject

@Composable
fun PlayerPanels(
  panelShown: Panels,
  onDismissRequest: () -> Unit,
  // Subtitle settings panel state
  isBold: Boolean,
  isItalic: Boolean,
  subJustify: SubtitleJustification,
  subFont: String,
  subFontSize: Int,
  subBorderStyle: SubtitlesBorderStyle,
  subBorderSize: Int,
  subShadowOffset: Int,
  subColor: SubColorType,
  currentSubtitleColor: Int,
  overrideAssSubs: Boolean,
  subScale: Float,
  subPos: Int,
  onSubBoldChange: (Boolean) -> Unit,
  onSubItalicChange: (Boolean) -> Unit,
  onSubJustifyChange: (SubtitleJustification) -> Unit,
  onSubFontChange: (String) -> Unit,
  onSubFontSizeChange: (Int) -> Unit,
  onSubBorderStyleChange: (SubtitlesBorderStyle) -> Unit,
  onSubBorderSizeChange: (Int) -> Unit,
  onSubShadowOffsetChange: (Int) -> Unit,
  onSubColorChange: (Int) -> Unit,
  onSubColorTypeChange: (SubColorType) -> Unit,
  onOverrideAssSubsChange: (Boolean) -> Unit,
  onSubScaleChange: (Float) -> Unit,
  onSubPosChange: (Int) -> Unit,
  onSubtitleSettingsReset: () -> Unit,
  onSubtitleMiscReset: () -> Unit,
  subDelayMsPrimary: Int,
  subDelayMsSecondary: Int,
  subSpeed: Double,
  onSubDelayPrimaryChange: (Int) -> Unit,
  onSubDelaySecondaryChange: (Int) -> Unit,
  onSubSpeedChange: (Double) -> Unit,
  onSubDelayApply: () -> Unit,
  onSubDelayReset: () -> Unit,
  onSubColorReset: (SubColorType) -> Unit,
  // Audio delay panel state
  audioDelayMs: Int,
  onAudioDelayChange: (Int) -> Unit,
  onAudioDelayApply: () -> Unit,
  onAudioDelayReset: () -> Unit,
  // Video settings panel state
  deband: Debanding,
  onDebandChange: (Debanding) -> Unit,
  onVideoFilterChange: (VideoFilters, Int) -> Unit,
  debandSettings: (DebandSettings) -> Int,
  onDebandSettingsChange: (DebandSettings, Int) -> Unit,
  onDebandReset: () -> Unit,
  isGpuNextEnabled: Boolean,
  filterValue: (VideoFilters) -> Int,
  onFilterReset: () -> Unit,
  modifier: Modifier,
) {
  AnimatedContent(
    targetState = panelShown,
    label = "panels",
    contentAlignment = Alignment.CenterEnd,
    contentKey = { it.name },
    transitionSpec = {
      fadeIn() + slideInHorizontally { it / 3 } togetherWith fadeOut() + slideOutHorizontally { it / 2 }
    },
    modifier = modifier,
  ) { currentPanel ->
    when (currentPanel) {
      Panels.None -> {
        Box(Modifier.fillMaxHeight())
      }

      Panels.SubtitleSettings -> {
        SubtitleSettingsPanel(
          onDismissRequest = onDismissRequest,
          isBold = isBold,
          isItalic = isItalic,
          justify = subJustify,
          font = subFont,
          fontSize = subFontSize,
          borderStyle = subBorderStyle,
          borderSize = subBorderSize,
          shadowOffset = subShadowOffset,
          onIsBoldChange = onSubBoldChange,
          onIsItalicChange = onSubItalicChange,
          onJustificationChange = onSubJustifyChange,
          onFontChange = onSubFontChange,
          onFontSizeChange = onSubFontSizeChange,
          onBorderStyleChange = onSubBorderStyleChange,
          onBorderSizeChange = onSubBorderSizeChange,
          onShadowOffsetChange = onSubShadowOffsetChange,
          onTypographyReset = onSubtitleSettingsReset,
          currentColorType = subColor,
          currentSubtitleColor = currentSubtitleColor,
          onColorChange = onSubColorChange,
          onColorReset = onSubColorReset,
          onColorTypeChange = onSubColorTypeChange,
          overrideAssSubs = overrideAssSubs,
          subScale = subScale,
          subPos = subPos,
          onOverrideAssSubsChange = onOverrideAssSubsChange,
          onSubScaleChange = onSubScaleChange,
          onSubPosChange = onSubPosChange,
          onMiscReset = onSubtitleMiscReset,
          modifier = Modifier,
        )
      }

      Panels.SubtitleDelay -> {
        SubtitleDelayPanel(
          delayMs = subDelayMsPrimary,
          secondaryDelayMs = subDelayMsSecondary,
          speed = subSpeed,
          onSpeedChange = onSubSpeedChange,
          onDelayChange = onSubDelayPrimaryChange,
          onSecondaryDelayChange = onSubDelaySecondaryChange,
          onApply = onSubDelayApply,
          onReset = onSubDelayReset,
          onDismissRequest = onDismissRequest,
        )
      }

      Panels.AudioDelay -> {
        AudioDelayPanel(
          delayMs = audioDelayMs,
          onDelayChange = onAudioDelayChange,
          onApply = onAudioDelayApply,
          onReset = onAudioDelayReset,
          onDismissRequest = onDismissRequest,
        )
      }

      Panels.VideoFilters -> {
        VideoSettingsPanel(
          onDismissRequest = onDismissRequest,
          onVideoFilterChange = onVideoFilterChange,
          deband = deband,
          onDebandChange = onDebandChange,
          debandSettings = debandSettings,
          onDebandSettingsChange = onDebandSettingsChange,
          onDebandReset = onDebandReset,
          isGpuNextEnabled = isGpuNextEnabled,
          filterValue = filterValue,
          onFilterReset = onFilterReset,
        )
      }
    }
  }
}

val CARDS_MAX_WIDTH = 420.dp
val panelCardsColors: @Composable () -> CardColors = {
  val playerPreferences = koinInject<PlayerPreferences>()

  val colors = CardDefaults.cardColors()
  colors.copy(
    containerColor = MaterialTheme.colorScheme.surface.copy(playerPreferences.panelTransparency.get()),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim.copy(playerPreferences.panelTransparency.get()),
  )
}
