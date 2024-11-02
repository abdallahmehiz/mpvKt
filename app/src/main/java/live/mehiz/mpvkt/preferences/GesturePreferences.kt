package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.player.DoubleTapGesture

class GesturePreferences(preferenceStore: PreferenceStore) {
  val doubleTapToSeekDuration = preferenceStore.getInt("double_tap_to_seek_duration", 10)
  val leftDoubleTapGesture = preferenceStore.getEnum("left_double_tap_gesture", DoubleTapGesture.Seek)
  val centerDoubleTapGesture = preferenceStore.getEnum("center_drag_gesture", DoubleTapGesture.PlayPause)
  val rightDoubleTapGesture = preferenceStore.getEnum("right_drag_gesture", DoubleTapGesture.Seek)
}
