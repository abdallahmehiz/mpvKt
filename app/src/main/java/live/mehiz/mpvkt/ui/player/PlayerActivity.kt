package live.mehiz.mpvkt.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.database.MpvKtDatabase
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.controls.PlayerControls
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import org.koin.android.ext.android.inject
import java.io.File

class PlayerActivity : AppCompatActivity() {

  private val viewModel: PlayerViewModel by lazy { PlayerViewModel(this) }
  private val binding by lazy { PlayerLayoutBinding.inflate(this.layoutInflater) }
  private val mpvKtDatabase: MpvKtDatabase by inject()
  val player by lazy { binding.player }
  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    if (playerPreferences.drawOverDisplayCutout.get()) enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(binding.root)

    setupMPV()
    setupAudio()
    setupSubtitles()
    val uri = parsePathFromIntent(intent)
    val videoUri = if (uri?.startsWith("content://") == true) {
      openContentFd(Uri.parse(uri))
    } else {
      uri
    }
    player.playFile(videoUri!!)
    setOrientation()
    val controls = PlayerControls(viewModel)

    binding.controls.setContent {
      MpvKtTheme {
        controls.Content()
      }
    }
  }

  override fun onDestroy() {
    MPVLib.destroy()
    super.onDestroy()
  }

  override fun onPause() {
    super.onPause()
    if (viewModel.paused.value) return
    viewModel.pauseUnpause()
  }

  private fun setupMPV() {

    Utils.copyAssets(this)
    copyMPVConfigFiles()

    player.initialize(
      applicationContext.filesDir.path,
      applicationContext.cacheDir.path,
      "v",
      if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu",
    )

    val statisticsPage = advancedPreferences.enabledStatisticsPage.get()
    if (statisticsPage != 0) {
      MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
      MPVLib.command(
        arrayOf("script-binding", "stats/display-page-$statisticsPage"),
      )
    }
    MPVLib.setPropertyString(
      "hwdec",
      if (decoderPreferences.tryHWDecoding.get()) "auto-copy" else "no",
    )
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MPVLib.setPropertyString("vf", "gradfun=radius=12")
      Debanding.GPU -> MPVLib.setPropertyString("deband", "yes")
    }
    if (decoderPreferences.useYUV420P.get()) {
      MPVLib.setPropertyString("vf", "format=yuv420p")
    }
    player.playbackSpeed = playerPreferences.defaultSpeed.get().toDouble()
    MPVLib.setPropertyString("keep-open", "yes")
    if (playerPreferences.savePositionOnQuit.get()) {
      MPVLib.setPropertyString("save-position-on-quit", "yes")
    }

    player.addObserver(PlayerObserver(this))
  }

  private fun setupAudio() {
    MPVLib.setPropertyString("alang", audioPreferences.preferredLanguages.get())
  }

  private fun setupSubtitles() {
    MPVLib.setPropertyString("slang", subtitlesPreferences.preferredLanguages.get())
  }

  private fun copyMPVConfigFiles() {
    val applicationPath = applicationContext.filesDir.path
    try {
      DocumentFile.fromTreeUri(this, Uri.parse(advancedPreferences.mpvConfStorageUri.get()))!!.listFiles().forEach {
        if (it.isDirectory) {
          DocumentFile.fromFile(File(applicationPath)).createDirectory(it.name!!)
          return@forEach
        }
        val input = contentResolver.openInputStream(it.uri)
        input!!.copyTo(File(applicationPath + "/" + it.name).outputStream())
      }
    } catch (e: Exception) {
      File(applicationContext.filesDir.path + "/mpv.conf").writeText(advancedPreferences.mpvConf.get())
      Log.e("PlayerActivity", "Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun setupIntents(intent: Intent) {
    viewModel.fileName = intent.getStringExtra("title") ?: ""
    player.timePos = intent.getIntExtra("position", 0) / 1000
  }

  private fun parsePathFromIntent(intent: Intent): String? {
    val filepath: String? = when (intent.action) {
      Intent.ACTION_VIEW -> intent.data?.let { resolveUri(it) }
      Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
        val uri = Uri.parse(it.trim())
        if (uri.isHierarchical && !uri.isRelative) resolveUri(uri) else null
      }

      else -> intent.getStringExtra("uri")
    }
    return filepath
  }

  private fun resolveUri(data: Uri): String? {
    val filepath = when (data.scheme) {
      "file" -> data.path
      "content" -> openContentFd(data)
      "http", "https", "rtmp", "rtmps", "rtp", "rtsp", "mms", "mmst", "mmsh", "tcp", "udp", "lavf",
      -> data.toString()

      else -> null
    }

    if (filepath == null) Log.e("mpvKt", "unknown scheme: ${data.scheme}")
    return filepath
  }

  fun openContentFd(uri: Uri): String? {
    if (uri.scheme != "content") return null
    val resolver = applicationContext.contentResolver
    Log.d("mpvKt", "Resolving content URI: $uri")
    val fd = try {
      val desc = resolver.openFileDescriptor(uri, "r")
      desc!!.detachFd()
    } catch (e: Exception) {
      Log.d("mpvKt", "Failed to open content fd: $e")
      return null
    }
    try {
      val path = File("/proc/self/fd/$fd").canonicalPath
      if (!path.startsWith("/proc") && File(path).canRead()) {
        Log.d("mpvKt", "Found real file path: $path")
        ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
        return path
      }
    } catch (_: Exception) {

    }
    // Else, pass the fd to mpv
    return "fdclose://$fd"
  }

  // a bunch of observers
  internal fun onObserverEvent(property: String, value: Long) {
    when (property) {
      "time-pos" -> viewModel.updatePlayBackPos(value.toFloat())
      "duration" -> viewModel.duration.update { value.toFloat() }
      "demuxer-cache-time" -> viewModel.updateReadAhead(value = value)
    }
  }

  internal fun onObserverEvent(property: String) {

  }

  internal fun onObserverEvent(property: String, value: Boolean) {
    when (property) {
      "paused-for-cache" -> {
        viewModel.isLoading.update { value }
      }

      "seeking" -> {
        viewModel.isLoading.update { value }
      }
    }
  }

  internal fun onObserverEvent(property: String, value: String) {

  }

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        CoroutineScope(Dispatchers.IO).launch {
          reuseVideoPlaybackState(MPVLib.getPropertyString("media-title"))
          if (intent.hasExtra("position")) setupIntents(intent)
        }
        setOrientation()
        viewModel.changeVideoAspect(playerPreferences.videoAspect.get())
        viewModel.duration.value = player.duration!!.toFloat()
        viewModel.loadChapters()
        viewModel.loadTracks()
        viewModel.getDecoder()
      }

      MPVLib.mpvEventId.MPV_EVENT_SEEK -> {
        viewModel.isLoading.update { true }
      }

      MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
        endPlayback(EndPlaybackReason.PlaybackCompleted)
      }
    }
  }

  private suspend fun saveVideoPlaybackState() {
    mpvKtDatabase.videoDataDao().upsert(
      PlaybackStateEntity(
        MPVLib.getPropertyString("media-title"),
        if (playerPreferences.savePositionOnQuit.get()) player.timePos ?: 0 else 0,
        player.sid,
        player.secondarySid,
        player.aid,
      ),
    )
  }

  private suspend fun reuseVideoPlaybackState(mediaTitle: String) {
    val state = mpvKtDatabase.videoDataDao().getVideoDataByTitle(mediaTitle)
    state?.let {
      player.timePos = it.lastPosition
      player.sid = it.sid
      player.secondarySid = it.secondarySid
      player.aid = it.aid
    }
  }

  override fun finish() {
    endPlayback(EndPlaybackReason.ExternalAction)
  }

  private fun endPlayback(reason: EndPlaybackReason) {
    CoroutineScope(Dispatchers.IO).launch {
      saveVideoPlaybackState()
    }
    if (!intent.getBooleanExtra("return_result", false)) {
      super.finish()
      return
    }
    val returnIntent = Intent()
    returnIntent.putExtra("end_by", reason.value)
    player.timePos?.let { returnIntent.putExtra("position", it * 1000) }
    player.duration?.let { returnIntent.putExtra("duration", it * 1000) }
    setResult(RESULT_OK, returnIntent)
    super.finish()
  }

  internal fun efEvent(err: String?) {

  }

  private fun setOrientation() {
    this.requestedOrientation = when (playerPreferences.orientation.get()) {
      PlayerOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
      PlayerOrientation.Video -> if ((player.videoAspect ?: 0.0) > 1.0) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      }

      PlayerOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      PlayerOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      PlayerOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      PlayerOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> { viewModel.changeVolumeBy(1) }
      KeyEvent.KEYCODE_VOLUME_DOWN -> { viewModel.changeVolumeBy(-1) }
      else -> { super.onKeyDown(keyCode, event) }
    }
    return true
  }
}
