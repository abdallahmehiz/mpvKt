package live.mehiz.mpvkt.ui.player

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode

class PlayerObserver(
  private val activity: PlayerActivity
) : MPVLib.EventObserver {
  override fun eventProperty(property: String) {
    activity.runOnUiThread { activity.onObserverEvent(property) }
  }

  override fun eventProperty(property: String, value: Long) {
    activity.runOnUiThread { activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(property: String, value: Boolean) {
    activity.runOnUiThread { activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(property: String, value: String) {
    activity.runOnUiThread { activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(property: String, value: Double) {
    activity.runOnUiThread { activity.onObserverEvent(property, value) }
  }

  @Suppress("EmptyFunctionBlock")
  override fun eventProperty(property: String, value: MPVNode) {
    activity.runOnUiThread { activity.onObserverEvent(property, value) }
  }

  override fun event(eventId: Int) {
    activity.runOnUiThread { activity.event(eventId) }
  }
}
