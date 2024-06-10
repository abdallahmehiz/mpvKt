package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore

class AdvancedPreferences(preferenceStore: PreferenceStore) {
  val mpvConfStorageUri = preferenceStore.getString("mpv_conf_storage_location_uri")
  val mpvConf = preferenceStore.getString("mpv.conf")

  val enabledStatisticsPage = preferenceStore.getInt("enabled_stats_page", 0)
}
