package live.mehiz.mpvkt.preferences

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import live.mehiz.mpvkt.domain.media.MediaType
import live.mehiz.mpvkt.domain.media.SortField
import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum

@Inject
@SingleIn(AppScope::class)
class BrowserPreferences(preferenceStore: PreferenceStore) {
  val sortField = preferenceStore.getEnum("browser_sort_field", SortField.Name)
  val sortAscending = preferenceStore.getBoolean("browser_sort_ascending", true)
  val mediaTypes = preferenceStore.getStringSet(
    "browser_media_types",
    setOf(MediaType.Video.name, MediaType.Audio.name),
  )
}
