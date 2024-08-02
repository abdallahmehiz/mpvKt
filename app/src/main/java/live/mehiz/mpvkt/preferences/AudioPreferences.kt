package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore

class AudioPreferences(preferenceStore: PreferenceStore) {
  val preferredLanguages = preferenceStore.getString("audio_preferred_languages")
  val defaultAudioDelay = preferenceStore.getInt("audio_delay_default")
  val audioPitchCorrection = preferenceStore.getBoolean("audio_pitch_correction", true)
}
