package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.player.PlayerOrientation
import live.mehiz.mpvkt.ui.player.VideoAspect

class PlayerPreferences(
  preferenceStore: PreferenceStore
) {
  val orientation = preferenceStore.getEnum("player_orientation", PlayerOrientation.SensorLandscape)
  val invertDuration = preferenceStore.getBoolean("invert_duration")
  val drawOverDisplayCutout = preferenceStore.getBoolean("draw_over_cutout", true)

  val doubleTapToPause = preferenceStore.getBoolean("double_tap_to_pause", true)
  val doubleTapToSeek = preferenceStore.getBoolean("double_tap_to_seek", true)
  val doubleTapToSeekDuration = preferenceStore.getInt("double_tap_to_seek_duration", 10)
  val holdForDoubleSpeed = preferenceStore.getBoolean("hold_for_double_speed", true)
  val horizontalSeekGesture = preferenceStore.getBoolean("horizontal_seek_gesture", true)
  val showSeekBarWhenSeeking = preferenceStore.getBoolean("show_seekbar_when_seeking")
  val preciseSeeking = preferenceStore.getBoolean("precise_seeking")

  val brightnessGesture = preferenceStore.getBoolean("gestures_brightness", true)
  val volumeGesture = preferenceStore.getBoolean("volume_brightness", true)

  val videoAspect = preferenceStore.getEnum("video_aspect", VideoAspect.Fit)
  val currentChaptersIndicator = preferenceStore.getBoolean("show_video_chapter_indicator", true)
  val showChaptersButton = preferenceStore.getBoolean("show_video_chapters_button")

  val defaultSpeed = preferenceStore.getFloat("default_speed", 1f)
  val speedPresets = preferenceStore.getStringSet(
    "default_speed_presets",
    setOf("0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0", "2.5", "3.0", "3.5", "4.0")
  )
  val displayVolumeAsPercentage = preferenceStore.getBoolean("display_volume_as_percentage", true)
  val showLoadingCircle = preferenceStore.getBoolean("show_loading_circle", true)
  val savePositionOnQuit = preferenceStore.getBoolean("save_position", true)

  val automaticallyEnterPip = preferenceStore.getBoolean("automatic_pip")
  val closeAfterReachingEndOfVideo = preferenceStore.getBoolean("close_after_eof")

  val rememberBrightness = preferenceStore.getBoolean("remember_rightness")
  val defaultBrightness = preferenceStore.getFloat("default_brightness", -1f)

  val allowGesturesInPanels = preferenceStore.getBoolean("allow_gestures_in_panels")
}
