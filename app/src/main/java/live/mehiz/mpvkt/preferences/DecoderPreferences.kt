package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore

class DecoderPreferences(preferenceStore: PreferenceStore) {
  val tryHWDecoding = preferenceStore.getBoolean("try_hw_dec", true)
}
