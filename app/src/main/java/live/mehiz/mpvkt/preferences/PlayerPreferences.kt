package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.player.PlayerOrientation
import live.mehiz.mpvkt.ui.player.VideoAspect

class PlayerPreferences(
  preferenceStore: PreferenceStore
) {
  val orientation = preferenceStore.getEnum("player_orientation", PlayerOrientation.SensorLandscape)
  val invertDuration = preferenceStore.getBoolean("invert_duration", false)
  val drawOverDisplayCutout = preferenceStore.getBoolean("draw_over_cutout", true)

  val doubleTapToPause = preferenceStore.getBoolean("double_tap_to_pause", false)
  val doubleTapToSeek = preferenceStore.getBoolean("double_tap_to_seek", true)
  val doubleTapToSeekDuration = preferenceStore.getInt("double_tap_to_seek_duration", 10)

  val horizontalSeekGesture = preferenceStore.getBoolean("horizontal_seek_gesture", true)
  val brightnessGesture = preferenceStore.getBoolean("gestures_brightness", true)
  val volumeGesture = preferenceStore.getBoolean("volume_brightness", true)

  val videoAspect = preferenceStore.getEnum("video_aspect", VideoAspect.Fit)
  val currentChaptersIndicator = preferenceStore.getBoolean("show_video_chapter_indicator", true)
  val showChaptersButton = preferenceStore.getBoolean("show_video_chapters_button", false)

  val defaultSpeed = preferenceStore.getFloat("default_speed", 1f)
  val savePositionOnQuit = preferenceStore.getBoolean("save_position", true)
}
