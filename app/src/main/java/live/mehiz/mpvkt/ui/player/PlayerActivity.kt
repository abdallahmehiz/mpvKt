package live.mehiz.mpvkt.ui.player

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
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
import live.mehiz.mpvkt.ui.player.controls.components.sheets.subtitles.toColorHexString
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.File

@Suppress("TooManyFunctions")
class PlayerActivity : AppCompatActivity() {

  private val viewModel: PlayerViewModel by lazy { PlayerViewModel(this) }
  private val binding by lazy { PlayerLayoutBinding.inflate(layoutInflater) }
  private val mpvKtDatabase: MpvKtDatabase by inject()
  val player by lazy { binding.player }
  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()

  private lateinit var fileName: String

  private var audioFocusRequest: AudioFocusRequestCompat? = null
  private var restoreAudioFocus: () -> Unit = {}

  private var pipRect: android.graphics.Rect? = null
  private val isPipSupported by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      false
    } else {
      packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
  }
  private var pipReceiver: BroadcastReceiver? = null

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
    loadKoinModules(module { viewModel { viewModel } })

    binding.controls.setContent {
      MpvKtTheme {
        PlayerControls(
          modifier = Modifier.onGloballyPositioned {
            pipRect = it.boundsInWindow().toAndroidRect()
          },
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    audioFocusRequest?.let {
      AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
    }
    audioFocusRequest = null
    MPVLib.destroy()
  }

  override fun finish() {
    endPlayback(EndPlaybackReason.ExternalAction)
    super.finish()
  }

  override fun onPause() {
    super.onPause()
    CoroutineScope(Dispatchers.IO).launch {
      saveVideoPlaybackState()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode) {
      viewModel.pause()
    } else {
      return
    }
  }

  override fun onUserLeaveHint() {
    if (!isPipSupported) {
      super.onUserLeaveHint()
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && player.paused == false) {
      enterPictureInPictureMode()
    }
    super.onUserLeaveHint()
  }

  override fun onStart() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setPictureInPictureParams(createPipParams())
    }
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    super.onStart()
  }

  private fun setupMPV() {
    Utils.copyAssets(this)
    lifecycleScope.launch(Dispatchers.IO) { copyMPVConfigFiles() }

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
      if (decoderPreferences.tryHWDecoding.get()) "auto" else "no",
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

    MPVLib.setPropertyString("input-default-bindings", "yes")

    player.addObserver(PlayerObserver(this))
  }

  private fun setupAudio() {
    MPVLib.setPropertyString("alang", audioPreferences.preferredLanguages.get())

    val request = AudioFocusRequestCompat
      .Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
      .also {
        it.setAudioAttributes(
          AudioAttributesCompat
            .Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build(),
        )
        it.setOnAudioFocusChangeListener(audioFocusChangeListener)
      }.build()
    AudioManagerCompat.requestAudioFocus(audioManager, request).let {
      if (it == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return@let
      audioFocusRequest = request
    }
  }

  private fun setupSubtitles() {
    lifecycleScope.launch(Dispatchers.IO) { copyMPVFonts() }

    MPVLib.setPropertyString("slang", subtitlesPreferences.preferredLanguages.get())
    MPVLib.setPropertyString("sub-ass-override", if (subtitlesPreferences.overrideAssSubs.get()) "force" else "no")

    MPVLib.setPropertyString("sub-fonts-dir", applicationContext.cacheDir.path + "/fonts/")
    MPVLib.setPropertyString("sub-font", subtitlesPreferences.font.get())

    MPVLib.setPropertyString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    MPVLib.setPropertyString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    MPVLib.setPropertyString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())

    MPVLib.setPropertyInt("sub-pos", subtitlesPreferences.position.get())
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
        input.close()
      }
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf").writeText(advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf").writeText(advancedPreferences.inputConf.get())
      Log.e("PlayerActivity", "Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun copyMPVFonts() {
    val cachePath = applicationContext.cacheDir.path
    val fontsDir = DocumentFile.fromFile(File("$cachePath/fonts"))
    if (!fontsDir.exists()) DocumentFile.fromFile(File(cachePath)).createDirectory("fonts")
    try {
      if (fontsDir.findFile("subfont.ttf")?.exists() != true) {
        applicationContext.resources.assets.open("subfont.ttf")
          .copyTo(File("$cachePath/fonts/subfont.ttf").outputStream())
      }
      DocumentFile.fromTreeUri(this, Uri.parse(subtitlesPreferences.fontsFolder.get()))?.listFiles()?.forEach {
        if (it.isDirectory || fontsDir.findFile(it.name!!)?.exists() == true) return
        val input = contentResolver.openInputStream(it.uri)
        input!!.copyTo(File("$cachePath/fonts/${it.name}").outputStream())
        input.close()
      }
    } catch (e: Exception) {
      Log.e("PlayerActivity", "Couldn't copy fonts to application directory: ${e.message}")
    }
  }

  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    when (it) {
      AudioManager.AUDIOFOCUS_LOSS,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
      -> {
        val oldRestore = restoreAudioFocus
        val wasPlayerPaused = player.paused ?: false
        viewModel.pause()
        restoreAudioFocus = {
          oldRestore()
          if (!wasPlayerPaused) viewModel.unpause()
        }
      }

      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        MPVLib.command(arrayOf("multiply", "volume", "0.5"))
        restoreAudioFocus = {
          MPVLib.command(arrayOf("multiply", "volume", "2"))
        }
      }

      AudioManager.AUDIOFOCUS_GAIN -> {
        restoreAudioFocus()
        restoreAudioFocus = {}
      }

      AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
        Log.d("PlayerActivity", "didn't get audio focus")
      }
    }
  }

  private fun setupIntents(intent: Intent) {
    intent.getStringExtra("title")?.let {
      viewModel.mediaTitle.update { _ -> it }
      MPVLib.setPropertyString("force-media-title", it)
    }
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

  @Suppress("ReturnCount")
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
    when (property) {
      "chapter-list" -> viewModel.loadChapters()
    }
  }

  internal fun onObserverEvent(property: String, value: Boolean) {
    when (property) {
      "pause" -> {
        if (value) viewModel.pause()
      }

      "paused-for-cache" -> {
        viewModel.isLoading.update { value }
      }

      "seeking" -> {
        viewModel.isLoading.update { value }
      }
    }
  }

  @Suppress("EmptyFunctionBlock", "UnusedParameter")
  internal fun onObserverEvent(property: String, value: String) {
  }

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        fileName = intent.data!!.lastPathSegment!!.substringAfterLast('/')
        viewModel.mediaTitle.update {
          MPVLib.getPropertyString("media-title").ifBlank { fileName }
        }
        CoroutineScope(Dispatchers.IO).launch {
          loadVideoPlaybackState(fileName)
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

  @Suppress("EmptyFunctionBlock", "UnusedParameter")
  internal fun efEvent(err: String?) {
  }

  private suspend fun saveVideoPlaybackState() {
    mpvKtDatabase.videoDataDao().upsert(
      PlaybackStateEntity(
        fileName,
        if (playerPreferences.savePositionOnQuit.get()) player.timePos ?: 0 else 0,
        player.sid,
        player.secondarySid,
        player.aid,
      ),
    )
  }

  private suspend fun loadVideoPlaybackState(mediaTitle: String) {
    val state = mpvKtDatabase.videoDataDao().getVideoDataByTitle(mediaTitle)
    state?.let {
      player.timePos = if (playerPreferences.savePositionOnQuit.get()) it.lastPosition else 0
      player.sid = it.sid
      player.secondarySid = it.secondarySid
      player.aid = it.aid
    }
  }

  private fun endPlayback(reason: EndPlaybackReason) {
    CoroutineScope(Dispatchers.IO).launch {
      saveVideoPlaybackState()
    }
    if (!intent.getBooleanExtra("return_result", false)) {
      return
    }
    val returnIntent = Intent()
    returnIntent.putExtra("end_by", reason.value)
    player.timePos?.let { returnIntent.putExtra("position", it * 1000) }
    player.duration?.let { returnIntent.putExtra("duration", it * 1000) }
    setResult(RESULT_OK, returnIntent)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun createPipParams(): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      builder.setTitle(viewModel.mediaTitle.value)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val autoEnter = playerPreferences.automaticallyEnterPip.get()
      builder.setAutoEnterEnabled(player.paused == false && autoEnter)
      builder.setSeamlessResizeEnabled(player.paused == false && autoEnter)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setActions(createPipActions(this, player.paused ?: true))
    }
    builder.setSourceRectHint(pipRect)
    player.videoH?.let {
      val height = it
      val width = it * player.getVideoOutAspect()!!
      val rational = Rational(height, width.toInt()).toFloat()
      if (rational in 0.41..2.40) builder.setAspectRatio(Rational(width.toInt(), height))
    }
    return builder.build()
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
    if (!isInPictureInPictureMode) {
      pipReceiver?.let {
        unregisterReceiver(pipReceiver)
        pipReceiver = null
      }
      super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setPictureInPictureParams(createPipParams())
    }
    viewModel.hideControls()
    viewModel.hideSeekBar()
    viewModel.isBrightnessSliderShown.update { false }
    viewModel.isVolumeSliderShown.update { false }
    viewModel.sheetShown.update { Sheets.None }
    pipReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action != PIP_INTENTS_FILTER) return
        when (intent.getIntExtra(PIP_INTENT_ACTION, 0)) {
          PIP_PAUSE -> viewModel.pause()
          PIP_PLAY -> viewModel.unpause()
          PIP_FF -> viewModel.seekBy(playerPreferences.doubleTapToSeekDuration.get())
          PIP_FR -> viewModel.seekBy(-playerPreferences.doubleTapToSeekDuration.get())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          setPictureInPictureParams(createPipParams())
        }
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(pipReceiver, IntentFilter(PIP_INTENTS_FILTER), RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(pipReceiver, IntentFilter(PIP_INTENTS_FILTER))
    }
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
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
      KeyEvent.KEYCODE_VOLUME_UP -> {
        viewModel.changeVolumeBy(1)
      }

      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        viewModel.changeVolumeBy(-1)
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        viewModel.seekBy(playerPreferences.doubleTapToSeekDuration.get())
      }

      KeyEvent.KEYCODE_DPAD_LEFT -> {
        viewModel.seekBy(-playerPreferences.doubleTapToSeekDuration.get())
      }

      KeyEvent.KEYCODE_SPACE -> {
        viewModel.pauseUnpause()
      }

      // other keys should be bound by the user in input.conf ig
      else -> {
        event?.let { player.onKey(it) }
        super.onKeyDown(keyCode, event)
      }
    }
    return true
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (player.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
  }
}
