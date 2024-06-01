package live.mehiz.mpvkt.ui.player

import androidx.annotation.StringRes
import live.mehiz.mpvkt.R

enum class PlayerOrientation(@StringRes val titleRes: Int) {
  Free(R.string.pref_player_orientation_free),
  Video(R.string.pref_player_orientation_video),
  Portrait(R.string.pref_player_orientation_portrait),
  ReversePortrait(R.string.pref_player_orientation_reverse_portrait),
  SensorPortrait(R.string.pref_player_orientation_sensor_portrait),
  Landscape(R.string.pref_player_orientation_landscape),
  ReverseLandscape(R.string.pref_player_orientation_reverse_landscape),
  SensorLandscape(R.string.pref_player_orientation_sensor_landscape),
  ;
}

enum class VideoAspect {
  Crop,
  Fit,
  Stretch,
  ;
}
