package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.player.PlayerOrientation

class PlayerPreferences(
  preferenceStore: PreferenceStore
) {
  val orientation = preferenceStore.getEnum("player_orientation", PlayerOrientation.SensorLandscape)
}
