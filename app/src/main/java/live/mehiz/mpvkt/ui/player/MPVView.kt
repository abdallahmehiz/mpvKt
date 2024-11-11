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
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KProperty

class MPVView(context: Context, attributes: AttributeSet) : BaseMPVView(context, attributes), KoinComponent {

  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()

  var isExiting = false

  val duration: Int?
    get() = MPVLib.getPropertyInt("duration")

  var timePos: Int?
    get() = MPVLib.getPropertyInt("time-pos")
    set(position) = MPVLib.setPropertyInt("time-pos", position!!)

  var paused: Boolean?
    get() = MPVLib.getPropertyBoolean("pause")
    set(paused) = MPVLib.setPropertyBoolean("pause", paused!!)

  val hwdecActive: String
    get() = MPVLib.getPropertyString("hwdec-current") ?: "no"

  var playbackSpeed: Double?
    get() = MPVLib.getPropertyDouble("speed")
    set(speed) = MPVLib.setPropertyDouble("speed", speed!!)

  var subDelay: Double?
    get() = MPVLib.getPropertyDouble("sub-delay")
    set(delay) = MPVLib.setPropertyDouble("sub-delay", delay!!)

  var secondarySubDelay: Double?
    get() = MPVLib.getPropertyDouble("secondary-sub-delay")
    set(delay) = MPVLib.setPropertyDouble("secondary-sub-delay", delay!!)

  val videoH: Int?
    get() = MPVLib.getPropertyInt("video-params/h")
  val videoAspect: Double?
    get() = MPVLib.getPropertyDouble("video-params/aspect")

  /**
   * Returns the video aspect ratio. Rotation is taken into account.
   */
  fun getVideoOutAspect(): Double? {
    return MPVLib.getPropertyDouble("video-params/aspect")?.let {
      if (it < 0.001) return 0.0
      if ((MPVLib.getPropertyInt("video-params/rotate") ?: 0) % 180 == 90) 1.0 / it else it
    }
  }

  class TrackDelegate(private val name: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
      val v = MPVLib.getPropertyString(name)
      // we can get null here for "no" or other invalid value
      return v?.toIntOrNull() ?: -1
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
      if (value == -1) {
        MPVLib.setPropertyString(name, "no")
      } else {
        MPVLib.setPropertyInt(name, value)
      }
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
    MPVLib.setOptionString("profile", "fast")
    MPVLib.setOptionString("hwdec", if (decoderPreferences.tryHWDecoding.get()) "auto" else "no")
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MPVLib.setOptionString("vf", "gradfun=radius=12")
      Debanding.GPU -> MPVLib.setOptionString("deband", "yes")
    }

    if (decoderPreferences.useYUV420P.get()) {
      MPVLib.setOptionString("vf", "format=yuv420p")
    }
    MPVLib.setOptionString("msg-level", "all=" + if (advancedPreferences.verboseLogging.get()) "v" else "warn")

    MPVLib.setPropertyBoolean("keep-open", true)
    MPVLib.setPropertyBoolean("input-default-bindings", true)

    MPVLib.setOptionString("tls-verify", "yes")
    MPVLib.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    // Limit demuxer cache since the defaults are too high for mobile devices
    val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
    MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
    //
    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    MPVLib.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      MPVLib.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    MPVLib.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    // workaround for <https://github.com/mpv-player/mpv/issues/14651>
    MPVLib.setOptionString("vd-lavc-film-grain", "cpu")

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
  }

  override fun postInitOptions() {
    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
        MPVLib.command(arrayOf("script-binding", "stats/display-page-$it"))
      }
    }
  }

  @Suppress("ReturnCount")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping.map.get(event.keyCode)
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
    MPVLib.command(arrayOf(action, mod.joinToString("+")))

    return true
  }

  private val observedProps = mapOf(
    "chapter" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
    "chapter-list" to MPVLib.mpvFormat.MPV_FORMAT_NONE,
    "track-list" to MPVLib.mpvFormat.MPV_FORMAT_NONE,

    "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
    "demuxer-cache-time" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
    "duration" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
    "volume" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
    "volume-max" to MPVLib.mpvFormat.MPV_FORMAT_INT64,

    "sid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "secondary-sid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "aid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,

    "speed" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    "video-params/aspect" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,

    "hwdec-current" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "hwdec" to MPVLib.mpvFormat.MPV_FORMAT_STRING,

    "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    "paused-for-cache" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    "seeking" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    "eof-reached" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,

    "user-data/mpvkt/show_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/toggle_ui" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/mpvkt/show_panel" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
  )

  private fun setupAudioOptions() {
    MPVLib.setOptionString("alang", audioPreferences.preferredLanguages.get())
    MPVLib.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    MPVLib.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
  }

  // Setup
  private fun setupSubtitlesOptions() {
    MPVLib.setOptionString("slang", subtitlesPreferences.preferredLanguages.get())

    MPVLib.setOptionString("sub-fonts-dir", context.cacheDir.path + "/fonts/")
    MPVLib.setOptionString("sub-delay", (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("sub-speed", subtitlesPreferences.defaultSubSpeed.get().toString())
    MPVLib.setOptionString(
      "secondary-sub-delay",
      (subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0).toString()
    )

    MPVLib.setOptionString("sub-font", subtitlesPreferences.font.get())
    if (subtitlesPreferences.overrideAssSubs.get()) {
      MPVLib.setOptionString("sub-ass-override", "force")
      MPVLib.setOptionString("sub-ass-justify", "yes")
    }
    MPVLib.setOptionString("sub-font-size", subtitlesPreferences.fontSize.get().toString())
    MPVLib.setOptionString("sub-bold", if (subtitlesPreferences.bold.get()) "yes" else "no")
    MPVLib.setOptionString("sub-italic", if (subtitlesPreferences.italic.get()) "yes" else "no")
    MPVLib.setOptionString("sub-justify", subtitlesPreferences.justification.get().value)
    MPVLib.setOptionString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    MPVLib.setOptionString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())
    MPVLib.setOptionString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    MPVLib.setOptionString("sub-border-size", subtitlesPreferences.borderSize.get().toString())
    MPVLib.setOptionString("sub-border-style", subtitlesPreferences.borderStyle.get().value)
    MPVLib.setOptionString("sub-shadow-offset", subtitlesPreferences.shadowOffset.get().toString())
    MPVLib.setOptionString("sub-pos", subtitlesPreferences.subPos.get().toString())
    MPVLib.setOptionString("sub-scale", subtitlesPreferences.subScale.get().toString())
  }
}
