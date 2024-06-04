package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore

class SubtitlesPreferences(preferenceStore: PreferenceStore) {
  val preferredLanguages = preferenceStore.getString("sub_preferred_languages")
}
