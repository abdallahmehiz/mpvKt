package live.mehiz.mpvkt.preferences

import androidx.annotation.StringRes
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum

class AudioPreferences(preferenceStore: PreferenceStore) {
  val preferredLanguages = preferenceStore.getString("audio_preferred_languages")
  val defaultAudioDelay = preferenceStore.getInt("audio_delay_default")
  val audioPitchCorrection = preferenceStore.getBoolean("audio_pitch_correction", true)
  val audioChannels = preferenceStore.getEnum("audio_channels", AudioChannels.AutoSafe)
  val volumeBoostCap = preferenceStore.getInt("audio_volume_boost_cap", 30)
}

enum class AudioChannels(@StringRes val title: Int, val property: String, val value: String) {
  Auto(R.string.pref_audio_channels_auto, "audio-channels", "audio-safe"),
  AutoSafe(R.string.pref_audio_channels_auto_safe, "audio-channels", "auto"),
  Mono(R.string.pref_audio_channels_mono, "audio-channels", "mono"),
  Stereo(R.string.pref_audio_channels_stereo, "audio-channels", "stereo"),
  ReverseStereo(R.string.pref_audio_channels_stereo_reversed, "af", "pan=[stereo|c0=c1|c1=c0]"),
}
