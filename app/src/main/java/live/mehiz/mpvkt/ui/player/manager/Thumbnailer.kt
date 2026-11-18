package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import android.net.Uri
import android.view.Surface
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.ui.player.openContentFd
import kotlin.time.Duration.Companion.seconds

class Thumbnailer(
  private val mainMpv: MPV,
  private val context: Context,
  private val decoderPreferences: DecoderPreferences,
  scope: CoroutineScope,
) {
  // mpv work runs on this single thread
  private val dispatcher = Dispatchers.IO.limitedParallelism(1)
  private val scope = CoroutineScope(scope.coroutineContext + dispatcher)
  private val lock = Any()

  private var mpv: MPV? = null
  private var surface: Surface? = null
  private var surfaceSize = "0x0"

  /** Uri the main player was opened with: fd:// can't be shared, so content:// gets reopened. */
  @Volatile
  var sourceUri: Uri? = null

  private val requested = MutableStateFlow<Int?>(null)
  private val _aspect = MutableStateFlow<Float?>(null)

  val aspect = _aspect.asStateFlow()

  init {
    // started in the background right away so the first drag doesn't pay for mpv's startup
    this.scope.launch { synchronized(lock) { mpv = createInstance() } }
    mainMpv.eventFlow
      .filter { it.first == MPV.mpvEvent.MPV_EVENT_FILE_LOADED }
      .onEach { load() }
      .launchIn(this.scope)
    // follows decoder switches made from the player UI too (maybe the player shows no frames with hwdec or sw?)
    mainMpv.propFlow<String>("hwdec")
      .filterNotNull()
      .onEach { hwdec -> mpv?.prop("hwdec", hwdec) }
      .launchIn(this.scope)
    // while a seek is decoding only the latest request survives
    requested
      .filterNotNull()
      .onEach(::seek)
      .launchIn(this.scope)
  }

  // Which second to show? set to null when not previewing
  fun request(seconds: Int?) {
    requested.value = seconds
  }

  fun attachSurface(surface: Surface, width: Int, height: Int) = synchronized(lock) {
    this.surface = surface
    surfaceSize = "${width}x$height"
    mpv?.attach()
    Unit
  }

  fun resizeSurface(width: Int, height: Int) = synchronized(lock) {
    surfaceSize = "${width}x$height"
    mpv?.prop("android-surface-size", surfaceSize)
    Unit
  }

  fun detachSurface() = synchronized(lock) {
    surface = null
    mpv?.run {
      prop("vo", "null")
      prop("force-window", "no")
      detachSurface()
    }
    Unit
  }

  fun close() {
    CoroutineScope(dispatcher).launch {
      synchronized(lock) {
        mpv?.close()
        mpv = null
      }
    }
  }

  private fun MPV.attach() {
    attachSurface(surface ?: return)
    prop("android-surface-size", surfaceSize)
    prop("force-window", "yes")
    prop("vo", if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
  }

  private fun load() {
    requested.value = null
    _aspect.value = null
    val path = mainMpv.getPropertyString("path")
    // local video only, like thumbfast's defaults: no network streams, audio files or cover art
    val file = when {
      path == null || (path.contains("://") && !path.startsWith("fd") && !path.startsWith("file://")) -> null
      mainMpv.getPropertyBoolean("demuxer-via-network") == true -> null
      mainMpv.getPropertyString("current-tracks/video/id") == null -> null
      mainMpv.getPropertyBoolean("current-tracks/video/image") == true -> null

      // our own fd, closed by mpv on unload
      path.startsWith("fd") ->
        sourceUri?.takeIf { it.scheme == "content" }?.openContentFd(context)?.replace("fd://", "fdclose://")

      else -> path
    }
    if (file == null) mpv?.command("stop") else mpv?.command("loadfile", file)
  }

  private fun createInstance(): MPV {
    // empty config dir so the user's mpv.conf and scripts don't run in here
    val dir = context.cacheDir.resolve("thumbnailer").apply { mkdirs() }.path
    return MPV(context, configDir = dir, cacheDir = dir).apply {
      setOptionString("profile", "fast")
      prop("msg-level", "all=error")
      // idle=yes so it never shutdown automatically
      prop("idle", "yes")
      prop("keep-open", "always")
      prop("vo", "null")
      prop("aid", "no")
      prop("sid", "no")
      prop("hwdec", mainMpv.getPropertyString("hwdec") ?: "no")
      if (decoderPreferences.useYUV420P.get()) prop("vf", "format=yuv420p")
      prop("vd-lavc-film-grain", "cpu")
      // speed over quality, the preview is tiny
      prop("hr-seek", "no")
      prop("vd-lavc-skiploopfilter", "all")
      prop("vd-lavc-fast", "yes")
      prop("vd-lavc-o", "lowres=2")
      prop("scale", "bilinear")
      prop("dscale", "bilinear")
      prop("cscale", "bilinear")
      prop("pause", true)
      propFlow<Boolean>("pause")
        .onEach { if (it == false) prop("pause", true) }
        .launchIn(scope)
      attach()
    }
  }

  // throttle requests
  private suspend fun seek(seconds: Int) {
    val instance = mpv ?: return
    val restarted = withTimeoutOrNull(2.seconds) {
      instance.eventFlow
        .onSubscription { instance.command("seek", seconds.toString(), "absolute+keyframes") }
        .first { it.first == MPV.mpvEvent.MPV_EVENT_PLAYBACK_RESTART }
    }
    if (restarted != null) _aspect.value = instance.getPropertyFloat("video-params/aspect")
  }
}
