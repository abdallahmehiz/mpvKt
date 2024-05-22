package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.MPVView.Chapter
import `is`.xyz.mpv.Utils

@Composable
fun SeekbarWithTimers(
  position: Float,
  duration: Float,
  readAheadValue: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  chapters: List<Chapter>? = null,
  timersInverted: Pair<Boolean, Boolean>,
  positionTimerOnClick: () -> Unit,
  durationTimerOnCLick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .height(48.dp)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    VideoTimer(
      value = position,
      timersInverted.first,
      modifier = Modifier.weight(0.1f),
      positionTimerOnClick,
    )
    Seeker(
      value = position,
      range = 0f..duration,
      onValueChange = onValueChange,
      onValueChangeFinished = onValueChangeFinished,
      readAheadValue = readAheadValue,
      segments = chapters?.map { it.toSegment() } ?: emptyList(),
      modifier = Modifier.weight(0.8f),
      colors = SeekerDefaults.seekerColors(
        progressColor = MaterialTheme.colorScheme.primary,
        thumbColor = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.background,
        readAheadColor = MaterialTheme.colorScheme.onBackground
      )
    )
    VideoTimer(
      value = if (timersInverted.second) position - duration else duration,
      isInverted = timersInverted.second,
      modifier = Modifier.weight(0.1f),
      durationTimerOnCLick,
    )
  }
}

@Composable
fun VideoTimer(
  value: Float,
  isInverted: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  Text(
    modifier = modifier
      .fillMaxHeight()
      .clickable(
        interactionSource = interactionSource,
        indication = rememberRipple(color = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick)
      .wrapContentHeight(Alignment.CenterVertically),
    text = Utils.prettyTime(value.toInt(), isInverted),
    color = Color.White,
    textAlign = TextAlign.Center,
  )
}

fun Chapter.toSegment(): Segment {
  return Segment(this.title ?: "", this.time.toFloat())
}

@Preview
@Composable
fun previewSeekBar() {
  SeekbarWithTimers(
      5f,
      20f,
      4f,
      {},
      {},
      null,
      Pair(true, true),
      {},
  ) { }
}
