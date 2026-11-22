package live.mehiz.mpvkt.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the player's own gestures must step aside for an accessibility service.
 *
 * The player is operated by a full-screen gesture layer: double tap to seek, drag for
 * volume/brightness, long press for temporary speed. That is exactly the vocabulary touch
 * exploration uses for something else — a double tap means "activate", a drag means "move focus" —
 * so with a reader running those gestures must not reach the player.
 *
 * A reader that works without touch exploration is not covered by
 * [AccessibilityManager.isTouchExplorationEnabled] alone (several third-party readers keep it off
 * and navigate by focus), so this also yields whenever any accessibility service is enabled.
 */
@Composable
fun rememberGesturesSuspended(): State<Boolean> {
  val context = LocalContext.current
  val manager =
    remember(context) {
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
  val suspended =
    remember(manager) {
      mutableStateOf(
        manager?.let { it.isEnabled || it.isTouchExplorationEnabled } == true,
      )
    }

  DisposableEffect(manager) {
    val refresh = {
      suspended.value = manager?.let { it.isEnabled || it.isTouchExplorationEnabled } == true
    }
    val touchListener = AccessibilityManager.TouchExplorationStateChangeListener { refresh() }
    val stateListener = AccessibilityManager.AccessibilityStateChangeListener { refresh() }
    manager?.addTouchExplorationStateChangeListener(touchListener)
    manager?.addAccessibilityStateChangeListener(stateListener)
    onDispose {
      manager?.removeTouchExplorationStateChangeListener(touchListener)
      manager?.removeAccessibilityStateChangeListener(stateListener)
    }
  }

  return suspended
}
