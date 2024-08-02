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
}

enum class VideoAspect(@StringRes val titleRes: Int) {
  Crop(R.string.player_aspect_crop),
  Fit(R.string.player_aspect_fit),
  Stretch(R.string.player_aspect_stretch),
}

enum class Decoder(val title: String, val value: String) {
  AutoCopy("Auto", "auto-copy"),
  Auto("Auto", "auto"),
  SW("SW", "no"),
  HW("HW", "mediacodec-copy"),
  HWPlus("HW+", "mediacodec"),
}

fun getDecoderFromValue(value: String): Decoder {
  return Decoder.entries.first { it.value == value }
}

enum class Debanding {
  None,
  CPU,
  GPU,
}

enum class Sheets {
  None,
  SubtitleTracks,
  SubtitleSettings,
  SubtitleDelay,
  AudioTracks,
  AudioDelay,
  Chapters,
  Decoders,
  More,
}

enum class PlayerUpdates {
  None,
  DoubleSpeed,
  AspectRatio,
}

enum class EndPlaybackReason(val value: String) {
  ExternalAction("external_action"),
  PlaybackCompleted("playback_completion"),
  Error("error"),
}
