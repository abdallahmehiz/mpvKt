package live.mehiz.mpvkt.ui.player

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPV
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.component2
import kotlin.collections.iterator

class MPVView(
  context: Context,
  attributes: AttributeSet?
) : BaseMPVView(
  context,
  attributes
),
  KoinComponent {

  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()

  var isExiting = false

  /**
   * Returns the video aspect ratio. Rotation is taken into account.
   */
  fun getVideoOutAspect(): Double? {
    return mpv.getPropertyDouble("video-params/aspect")?.let {
      if (it < 0.001) return 0.0
      if ((mpv.getPropertyInt("video-params/rotate") ?: 0) % 180 == 90) 1.0 / it else it
    }
  }

  override fun initOptions() {
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
    mpv.setOptionString("profile", "fast")
    mpv.setOptionString("hwdec", if (decoderPreferences.tryHWDecoding.get()) "auto" else "no")

    if (decoderPreferences.useYUV420P.get()) {
      mpv.setOptionString("vf", "format=yuv420p")
    }
    mpv.setOptionString("msg-level", "all=" + if (advancedPreferences.verboseLogging.get()) "v" else "warn")

    mpv.setPropertyBoolean("keep-open", true)
    mpv.setPropertyBoolean("input-default-bindings", true)

    mpv.setOptionString("tls-verify", "yes")
    mpv.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    // Limit demuxer cache since the defaults are too high for mobile devices
    val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
    mpv.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    mpv.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
    //
    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    mpv.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      mpv.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    mpv.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    // workaround for <https://github.com/mpv-player/mpv/issues/14651>
    mpv.setOptionString("vd-lavc-film-grain", "cpu")

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) mpv.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> mpv.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> mpv.setOptionString("deband", "yes")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        mpv.command("script-binding", "stats/display-stats-toggle")
        mpv.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        if (event.repeatCount == 0) {
          Log.d(TAG, "Unmapped non-printable key ${event.keyCode}")
        }
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true // eat event but ignore it, mpv has its own key repeat
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    mpv.command(action, mod.joinToString("+"))

    return true
  }

  private val observedProps = mapOf(
    "pause" to MPV.mpvFormat.MPV_FORMAT_FLAG,
    "video-params/aspect" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
    "eof-reached" to MPV.mpvFormat.MPV_FORMAT_FLAG,

    "user-data/mpvkt/show_text" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/toggle_ui" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/show_panel" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/set_button_title" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/reset_button_title" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/toggle_button" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/seek_by" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/seek_to" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/seek_by_with_text" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/seek_to_with_text" to MPV.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/software_keyboard" to MPV.mpvFormat.MPV_FORMAT_STRING,
  )

  private fun setupAudioOptions() {
    mpv.setOptionString("alang", audioPreferences.preferredLanguages.get())
    mpv.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    mpv.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    mpv.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
  }

  // Setup
  private fun setupSubtitlesOptions() {
    mpv.setOptionString("slang", subtitlesPreferences.preferredLanguages.get())

    mpv.setOptionString("sub-fonts-dir", context.cacheDir.path + "/fonts/")
    mpv.setOptionString("sub-delay", (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString())
    mpv.setOptionString("sub-speed", subtitlesPreferences.defaultSubSpeed.get().toString())
    mpv.setOptionString(
      "secondary-sub-delay",
      (subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0).toString()
    )

    mpv.setOptionString("sub-font", subtitlesPreferences.font.get())
    if (subtitlesPreferences.overrideAssSubs.get()) {
      mpv.setOptionString("sub-ass-override", "force")
      mpv.setOptionString("sub-ass-justify", "yes")
    }
    mpv.setOptionString("sub-font-size", subtitlesPreferences.fontSize.get().toString())
    mpv.setOptionString("sub-bold", if (subtitlesPreferences.bold.get()) "yes" else "no")
    mpv.setOptionString("sub-italic", if (subtitlesPreferences.italic.get()) "yes" else "no")
    mpv.setOptionString("sub-justify", subtitlesPreferences.justification.get().value)
    mpv.setOptionString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    mpv.setOptionString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())
    mpv.setOptionString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    mpv.setOptionString("sub-border-size", subtitlesPreferences.borderSize.get().toString())
    mpv.setOptionString("sub-border-style", subtitlesPreferences.borderStyle.get().value)
    mpv.setOptionString("sub-shadow-offset", subtitlesPreferences.shadowOffset.get().toString())
    mpv.setOptionString("sub-pos", subtitlesPreferences.subPos.get().toString())
    mpv.setOptionString("sub-scale", subtitlesPreferences.subScale.get().toString())
  }
}
