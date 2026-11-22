package live.mehiz.mpvkt.accessibility

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Announces short-lived player feedback — temporary speed boost, aspect ratio, script text.
 *
 * Those changes are drawn as a bubble on top of the video, which a screen reader cannot see and
 * would otherwise not report at all. The bubble is excluded from the accessibility tree (see
 * [clearOverlaySemantics]) and the same information is announced from here instead.
 *
 * Announcements are coalesced: an unchanged message within [REPEAT_GUARD_MS] is dropped and a new
 * one replaces the pending message, so repeated gestures do not build a backlog.
 */
class StatusAnnouncer(private val view: View, private val manager: AccessibilityManager?) {
  private val handler = Handler(Looper.getMainLooper())

  private var lastMessage = ""
  private var lastAnnouncedAt = 0L
  private var pending: Runnable? = null

  @Suppress("ReturnCount")
  fun announce(message: String?) {
    if (message.isNullOrBlank()) return
    val accessibility = manager ?: return
    if (!accessibility.isEnabled) return

    val now = SystemClock.uptimeMillis()
    if (message == lastMessage && now - lastAnnouncedAt < REPEAT_GUARD_MS) return

    cancelPending()
    val runnable =
      Runnable {
        pending = null
        lastMessage = message
        lastAnnouncedAt = SystemClock.uptimeMillis()
        runCatching { accessibility.interrupt() }
        runCatching { view.announceForAccessibility(message) }
      }
    pending = runnable
    handler.postDelayed(runnable, THROTTLE_MS)
  }

  private fun cancelPending() {
    pending?.let(handler::removeCallbacks)
    pending = null
  }

  private companion object {
    /** Long enough that a repeated gesture collapses into one announcement. */
    const val THROTTLE_MS = 300L

    /** An unchanged value is not repeated faster than this. */
    const val REPEAT_GUARD_MS = 1_500L
  }
}

@Composable
fun rememberStatusAnnouncer(): StatusAnnouncer {
  val view = LocalView.current
  val context = LocalContext.current
  val manager =
    remember(context) {
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
  return remember(view, manager) { StatusAnnouncer(view, manager) }
}

/**
 * Removes a purely visual, transient overlay from the accessibility tree.
 *
 * The overlay duplicates what [StatusAnnouncer] says; leaving it in the tree would only add
 * unlabelled nodes to the swipe order while the user is trying to reach the controls.
 */
fun Modifier.clearOverlaySemantics(): Modifier = this.clearAndSetSemantics {}
