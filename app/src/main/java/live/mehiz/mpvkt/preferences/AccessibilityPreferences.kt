package live.mehiz.mpvkt.preferences

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import live.mehiz.mpvkt.preferences.preference.PreferenceStore

/**
 * Preferences for the screen reader layer.
 *
 * These only control what the player announces; they never change what mpv renders.
 */
@Inject
@SingleIn(AppScope::class)
class AccessibilityPreferences(preferenceStore: PreferenceStore) {
  /**
   * Speak the current subtitle line through accessibility events.
   *
   * On by default in this fork: an announcement is only produced while an accessibility service is
   * actually running, so this is silent for everyone else.
   */
  val announceSubtitles = preferenceStore.getBoolean("a11y_announce_subtitles", true)

  /** Also speak the secondary subtitle track, for dual-subtitle releases. */
  val announceSecondary = preferenceStore.getBoolean("a11y_announce_secondary", true)

  /**
   * Replace the line being spoken instead of queueing behind it. Keeps speech in sync with the
   * picture.
   */
  val interruptOnNewLine = preferenceStore.getBoolean("a11y_interrupt_on_new_line", true)

  /**
   * Stay quiet while subtitles are hidden: mpv keeps the current cue available even when it is not
   * drawn, and reading lines the user cannot see is confusing.
   */
  val onlyWhenVisible = preferenceStore.getBoolean("a11y_only_when_visible", true)
}
