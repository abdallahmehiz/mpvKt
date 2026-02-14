package live.mehiz.mpvkt.ui.player.controls.components.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitleJustification
import live.mehiz.mpvkt.ui.player.controls.components.panels.components.MultiCardPanel

@Composable
fun SubtitleSettingsPanel(
  onDismissRequest: () -> Unit,
  // Typography card state
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
  onTypographyReset: () -> Unit,
  // Colors card state
  currentSubtitleColor: Int,
  currentColorType: SubColorType,
  onColorChange: (Int) -> Unit,
  onColorReset: (SubColorType) -> Unit,
  onColorTypeChange: (SubColorType) -> Unit,
  // Misc card state
  overrideAssSubs: Boolean,
  subScale: Float,
  subPos: Int,
  onOverrideAssSubsChange: (Boolean) -> Unit,
  onSubScaleChange: (Float) -> Unit,
  onSubPosChange: (Int) -> Unit,
  onMiscReset: () -> Unit,
  modifier: Modifier,
) {
  MultiCardPanel(
    onDismissRequest = onDismissRequest,
    titleRes = R.string.player_sheets_subtitles_settings_title,
    cardCount = 3,
    modifier = modifier,
  ) { index, cardModifier ->
    when (index) {
      0 -> SubtitleSettingsTypographyCard(
        isBold = isBold,
        isItalic = isItalic,
        justify = justify,
        font = font,
        fontSize = fontSize,
        borderStyle = borderStyle,
        borderSize = borderSize,
        shadowOffset = shadowOffset,
        onIsBoldChange = onIsBoldChange,
        onIsItalicChange = onIsItalicChange,
        onJustificationChange = onJustificationChange,
        onFontChange = onFontChange,
        onFontSizeChange = onFontSizeChange,
        onBorderStyleChange = onBorderStyleChange,
        onBorderSizeChange = onBorderSizeChange,
        onShadowOffsetChange = onShadowOffsetChange,
        onReset = onTypographyReset,
        modifier = cardModifier,
      )
      1 -> {
        SubtitleSettingsColorsCard(
          currentColor = currentSubtitleColor,
          currentColorType = currentColorType,
          onColorChange = onColorChange,
          onColorReset = onColorReset,
          onColorTypeChange = onColorTypeChange,
          modifier = cardModifier,
        )
      }
      2 -> SubtitlesMiscellaneousCard(
        overrideAssSubs = overrideAssSubs,
        subScale = subScale,
        subPos = subPos,
        onOverrideAssSubsChange = onOverrideAssSubsChange,
        onSubScaleChange = onSubScaleChange,
        onSubPosChange = onSubPosChange,
        onReset = onMiscReset,
        modifier = cardModifier,
      )
      else -> {}
    }
  }
}
