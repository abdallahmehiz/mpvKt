package live.mehiz.mpvkt.ui.player.manager

import live.mehiz.mpvkt.ui.player.mapState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import androidx.core.graphics.toColorInt
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import live.mehiz.mpvkt.preferences.SubtitleJustification
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubColorType
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitlesBorderStyle
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
class SubtitleSettings(
  private val mpv: MPV,
  private val preferences: SubtitlesPreferences,
  scope: CoroutineScope,
) {
  val bold: StateFlow<Boolean> = mpv.propFlow("sub-bold", preferences.bold.get())
  val italic: StateFlow<Boolean> = mpv.propFlow("sub-italic", preferences.italic.get())
  val justification: StateFlow<SubtitleJustification> = mpv.propFlow<String>("sub-justify")
    .mapState(scope, preferences.justification::get, SubtitleJustification::byValue)
  val font: StateFlow<String> = mpv.propFlow("sub-font", preferences.font.get())
  val fontSize: StateFlow<Int> = mpv.propFlow("sub-font-size", preferences.fontSize.get())
  val borderStyle: StateFlow<SubtitlesBorderStyle> = mpv.propFlow<String>("sub-border-style")
    .mapState(scope, preferences.borderStyle::get, SubtitlesBorderStyle::byValue)
  val borderSize: StateFlow<Int> = mpv.propFlow("sub-border-size", preferences.borderSize.get())
  val shadowOffset: StateFlow<Int> = mpv.propFlow("sub-shadow-offset", preferences.shadowOffset.get())

  // a choice, not a flag
  val overrideAss: StateFlow<Boolean> = mpv.propFlow<String>("sub-ass-override")
    .mapState(scope, preferences.overrideAssSubs::get) { it == "force" }
  val scale: StateFlow<Float> = mpv.propFlow("sub-scale", preferences.subScale.get())
  val position: StateFlow<Int> = mpv.propFlow("sub-pos", preferences.subPos.get())

  val colors: Map<SubColorType, StateFlow<Int>> = SubColorType.entries.associateWith { type ->
    mpv.propFlow<String>(type.property).mapState(scope, { type.preference(preferences).get() }) { it.toColorInt() }
  }

  val delayMs: StateFlow<Int> = mpv.propFlow<Double>("sub-delay")
    .mapState(scope, preferences.defaultSubDelay::get) { (it * 1000).roundToInt() }
  val secondaryDelayMs: StateFlow<Int> = mpv.propFlow<Double>("secondary-sub-delay")
    .mapState(scope, preferences.defaultSecondarySubDelay::get) { (it * 1000).roundToInt() }
  val speed: StateFlow<Double> = mpv.propFlow("sub-speed", preferences.defaultSubSpeed.get().toDouble())

  fun setBold(value: Boolean) {
    mpv.prop("sub-bold", value)
    preferences.bold.set(value)
  }

  fun setItalic(value: Boolean) {
    mpv.prop("sub-italic", value)
    preferences.italic.set(value)
  }

  fun setJustification(value: SubtitleJustification) {
    mpv.prop("sub-justify", value.value)
    preferences.justification.set(value)
  }

  fun setFont(value: String) {
    mpv.prop("sub-font", value)
    preferences.font.set(value)
  }

  fun setFontSize(value: Int) {
    mpv.prop("sub-font-size", value)
    preferences.fontSize.set(value)
  }

  fun setBorderStyle(value: SubtitlesBorderStyle) {
    mpv.prop("sub-border-style", value.value)
    preferences.borderStyle.set(value)
  }

  fun setBorderSize(value: Int) {
    mpv.prop("sub-border-size", value)
    preferences.borderSize.set(value)
  }

  fun setShadowOffset(value: Int) {
    mpv.prop("sub-shadow-offset", value)
    preferences.shadowOffset.set(value)
  }

  fun setColor(type: SubColorType, color: Int) {
    mpv.prop(type.property, color.toColorHexString())
    type.preference(preferences).set(color)
  }

  fun setOverrideAss(value: Boolean) {
    mpv.prop("sub-ass-override", if (value) "force" else "scale")
    preferences.overrideAssSubs.set(value)
  }

  fun setScale(value: Float) {
    mpv.prop("sub-scale", value)
    preferences.subScale.set(value)
  }

  fun setPosition(value: Int) {
    mpv.prop("sub-pos", value)
    preferences.subPos.set(value)
  }

  fun resetColor(type: SubColorType) {
    mpv.prop(type.property, type.preference(preferences).deleteAndGet().toColorHexString())
  }

  fun resetTypography() {
    mpv.prop("sub-bold", preferences.bold.deleteAndGet())
    mpv.prop("sub-italic", preferences.italic.deleteAndGet())
    mpv.prop("sub-ass-justify", preferences.overrideAssSubs.deleteAndGet())
    mpv.prop("sub-justify", preferences.justification.deleteAndGet().value)
    mpv.prop("sub-font", preferences.font.deleteAndGet())
    mpv.prop("sub-font-size", preferences.fontSize.deleteAndGet())
    mpv.prop("sub-border-size", preferences.borderSize.deleteAndGet())
    mpv.prop("sub-shadow-offset", preferences.shadowOffset.deleteAndGet())
    mpv.prop("sub-border-style", preferences.borderStyle.deleteAndGet().value)
  }

  fun resetMisc() {
    mpv.prop("sub-pos", preferences.subPos.deleteAndGet())
    mpv.prop("sub-scale", preferences.subScale.deleteAndGet())
    preferences.overrideAssSubs.delete()
    mpv.prop("sub-ass-override", "scale")
  }

  fun setDelay(ms: Int) {
    mpv.prop("sub-delay", ms / 1000.0)
  }

  fun setSecondaryDelay(ms: Int) {
    mpv.prop("secondary-sub-delay", ms / 1000.0)
  }

  fun setSpeed(value: Double) {
    mpv.prop("sub-speed", value)
  }

  fun saveDelaysAsDefault() {
    preferences.defaultSubDelay.set(delayMs.value)
    preferences.defaultSecondarySubDelay.set(secondaryDelayMs.value)
  }

  fun resetTiming() {
    setDelay(preferences.defaultSubDelay.get())
    setSecondaryDelay(preferences.defaultSecondarySubDelay.get())
    setSpeed(preferences.defaultSubSpeed.get().toDouble())
  }
}
