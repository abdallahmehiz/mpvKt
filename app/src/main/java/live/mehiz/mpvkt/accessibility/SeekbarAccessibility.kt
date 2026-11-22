package live.mehiz.mpvkt.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.roundToInt

/**
 * Publishes a seek bar as a real slider for accessibility services.
 *
 * The seek bar comes from the seeker library, so this attaches from the outside rather than
 * changing the widget: a progress node plus the three things a listener needs — set an absolute
 * position, jump forward, jump back.
 *
 * The published value is quantised to whole seconds. Playback position arrives several times a
 * second, and a semantics value that changes that often makes the accessibility service re-read
 * the tree on every change, which turns the player into a stutter for the user it is meant to help.
 */
fun Modifier.seekbarAccessibility(
  position: Float,
  duration: Float,
  onSeek: (Float) -> Unit,
  onSeekFinished: () -> Unit,
  contentLabel: String,
  forwardLabel: String,
  backLabel: String,
  startLabel: String,
  stepSeconds: Int = 10,
): Modifier {
  if (duration <= 0f) return this

  val clamped = position.coerceIn(0f, duration)
  val progress = (clamped / PROGRESS_GRANULARITY_SECONDS).toInt() * PROGRESS_GRANULARITY_SECONDS

  val seekTo: (Float) -> Unit = { seconds ->
    onSeek(seconds.coerceIn(0f, duration))
    onSeekFinished()
  }

  return this.semantics {
    contentDescription = contentLabel
    progressBarRangeInfo =
      ProgressBarRangeInfo(
        current = progress.toFloat(),
        range = 0f..duration,
        steps = (duration / stepSeconds).roundToInt().coerceAtLeast(0),
      )
    setProgress { target ->
      seekTo(target)
      true
    }
    customActions =
      listOf(
        CustomAccessibilityAction(forwardLabel) {
          seekTo(clamped + stepSeconds)
          true
        },
        CustomAccessibilityAction(backLabel) {
          seekTo(clamped - stepSeconds)
          true
        },
        CustomAccessibilityAction(startLabel) {
          seekTo(0f)
          true
        },
      )
  }
}

/** Only whole seconds are published; see the note in the modifier. */
private const val PROGRESS_GRANULARITY_SECONDS = 1f
