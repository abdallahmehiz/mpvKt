package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.theme.spacing
import kotlin.math.abs

fun percentage(value: Float, range: ClosedFloatingPointRange<Float>): Float {
  return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

fun percentage(value: Int, range: ClosedRange<Int>): Float {
  return ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
) {
  require(range.contains(value)) { "Value must be within the provided range" }
  Box(
    modifier = modifier
      .height(120.dp)
      .aspectRatio(0.2f)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(value, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .background(MaterialTheme.colorScheme.tertiary),
    )
    if (overflowRange != null && overflowValue != null) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue, overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .background(MaterialTheme.colorScheme.errorContainer),
      )
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
) {
  require(range.contains(value)) { "Value must be within the provided range" }
  Box(
    modifier = modifier
      .height(120.dp)
      .aspectRatio(0.2f)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(value, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .background(MaterialTheme.colorScheme.tertiary),
    )
    if (overflowRange != null && overflowValue != null) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue, overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .background(MaterialTheme.colorScheme.errorContainer),
      )
    }
  }
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    Text(
      (brightness * 100).toInt().toString(),
      style = MaterialTheme.typography.bodySmall,
    )
    VerticalSlider(
      brightness,
      range,
    )
    Icon(
      when (percentage(brightness, range)) {
        in 0f..0.3f -> Icons.Default.BrightnessLow
        in 0.3f..0.6f -> Icons.Default.BrightnessMedium
        in 0.6f..1f -> Icons.Default.BrightnessHigh
        else -> Icons.Default.BrightnessMedium
      },
      null,
    )
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    val boostVolume = mpvVolume - 100
    Text(
      when (mpvVolume - 100) {
        0 -> "$volume"
        in 0..1000 -> "$volume + $boostVolume"
        in -100..-1 -> "$volume - ${abs(boostVolume)}"
        else -> "$volume ($mpvVolume)"
      },
      style = MaterialTheme.typography.bodySmall,
    )
    VerticalSlider(
      volume,
      range,
      overflowValue = boostVolume,
      overflowRange = boostRange,
    )
    Icon(
      when (percentage(volume, range)) {
        0f -> Icons.AutoMirrored.Default.VolumeOff
        in 0f..0.3f -> Icons.AutoMirrored.Default.VolumeMute
        in 0.3f..0.6f -> Icons.AutoMirrored.Default.VolumeDown
        in 0.6f..1f -> Icons.AutoMirrored.Default.VolumeUp
        else -> Icons.AutoMirrored.Default.VolumeOff
      },
      null,
    )
  }
}
