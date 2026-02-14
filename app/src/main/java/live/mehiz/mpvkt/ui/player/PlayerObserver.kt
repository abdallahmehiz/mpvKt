package live.mehiz.mpvkt.ui.player

import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode

class PlayerObserver(
  private val activity: PlayerActivity
) : MPV.EventObserver {
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

  override fun event(eventId: Int, data: MPVNode) {
    activity.runOnUiThread { activity.event(eventId, data) }
  }
}
