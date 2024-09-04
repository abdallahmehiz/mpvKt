package live.mehiz.mpvkt.ui.player

import android.annotation.SuppressLint
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
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.text.isDigitsOnly
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.github.k1rakishou.fsaf.FileManager
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
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
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

@Suppress("TooManyFunctions")
class PlayerActivity : AppCompatActivity() {

  private val viewModel: PlayerViewModel by lazy { PlayerViewModel(this) }
  private val viewModelModule: Module by lazy { module { viewModel { viewModel } } }
  private val binding by lazy { PlayerLayoutBinding.inflate(layoutInflater) }
  private val playerObserver by lazy { PlayerObserver(this) }
  private val mpvKtDatabase: MpvKtDatabase by inject()
  val player by lazy { binding.player }
  private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val fileManager: FileManager by inject()

  private lateinit var fileName: String

  private var audioFocusRequest: AudioFocusRequestCompat? = null
  private var restoreAudioFocus: () -> Unit = {}

  private var pipRect: android.graphics.Rect? = null
  val isPipSupported by lazy {
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
    setContentView(binding.root)

    setupMPV()
    setupAudio()
    setupSubtitles()
    val uri = parsePathFromIntent(intent)
    val videoUri = if (uri?.startsWith("content://") == true) openContentFd(Uri.parse(uri)) else uri
    player.playFile(videoUri!!)
    setOrientation()
    loadKoinModules(viewModelModule)

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
    Log.d(TAG, "Exiting")
    audioFocusRequest?.let {
      AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
    }
    audioFocusRequest = null

    unloadKoinModules(viewModelModule)
    MPVLib.removeObserver(playerObserver)
    MPVLib.destroy()

    super.onDestroy()
  }

  override fun onPause() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode) {
      viewModel.pause()
    }
    lifecycleScope.launch(Dispatchers.IO) {
      if ((player.timePos ?: 0) < (player.duration ?: 0)) saveVideoPlaybackState(false)
    }
    super.onPause()
  }

  override fun onStop() {
    viewModel.pause()
    player.isExiting = true
    super.onStop()
  }

  @SuppressLint("NewApi")
  override fun onUserLeaveHint() {
    if (isPipSupported && player.paused == false && playerPreferences.automaticallyEnterPip.get()) {
      enterPictureInPictureMode()
    }
    super.onUserLeaveHint()
  }

  @SuppressLint("NewApi")
  override fun onBackPressed() {
    if (isPipSupported && player.paused == false && playerPreferences.automaticallyEnterPip.get()) {
      if (viewModel.sheetShown.value == Sheets.None && viewModel.panelShown.value == Panels.None) {
        enterPictureInPictureMode()
      }
    } else {
      super.onBackPressed()
    }
  }

  override fun onStart() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setPictureInPictureParams(createPipParams())
    }
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    binding.root.systemUiVisibility =
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
      View.SYSTEM_UI_FLAG_LOW_PROFILE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    super.onStart()
  }

  private fun setupMPV() {
    Utils.copyAssets(this)
    lifecycleScope.launch(Dispatchers.IO) { copyMPVConfigFiles() }

    player.initialize(filesDir.path, cacheDir.path)

    val statisticsPage = advancedPreferences.enabledStatisticsPage.get()
    if (statisticsPage != 0) {
      MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
      MPVLib.command(
        arrayOf("script-binding", "stats/display-page-$statisticsPage"),
      )
    }

    VideoFilters.entries.forEach {
      MPVLib.setPropertyInt(it.mpvProperty, it.preference(decoderPreferences).get())
    }

    player.playbackSpeed = playerPreferences.defaultSpeed.get().toDouble()

    MPVLib.addObserver(playerObserver)
  }

  private fun setupAudio() {
    MPVLib.setPropertyString("alang", audioPreferences.preferredLanguages.get())
    MPVLib.setPropertyDouble("audio-delay", audioPreferences.defaultAudioDelay.get() / 1000.0)
    MPVLib.setPropertyBoolean("audio-pitch-correction", audioPreferences.audioPitchCorrection.get())
    audioPreferences.audioChannels.get().let { MPVLib.setPropertyString(it.property, it.value) }

    val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).also {
      it.setAudioAttributes(
        AudioAttributesCompat.Builder().setUsage(AudioAttributesCompat.USAGE_MEDIA)
          .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC).build(),
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
    subtitlesPreferences.overrideAssSubs.get().let {
      MPVLib.setPropertyString("sub-ass-override", if (it) "force" else "no")
      MPVLib.setPropertyBoolean("sub-ass-justify", it)
    }

    MPVLib.setPropertyString("sub-fonts-dir", cacheDir.path + "/fonts/")
    MPVLib.setPropertyString("sub-font", subtitlesPreferences.font.get())

    MPVLib.setPropertyInt("sub-font-size", subtitlesPreferences.fontSize.get())
    MPVLib.setPropertyBoolean("sub-bold", subtitlesPreferences.bold.get())
    MPVLib.setPropertyBoolean("sub-italic", subtitlesPreferences.italic.get())
    MPVLib.setPropertyString("sub-justify", subtitlesPreferences.justification.get().value)
    MPVLib.setPropertyString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    MPVLib.setPropertyInt("sub-border-size", subtitlesPreferences.borderSize.get())
    MPVLib.setPropertyString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    MPVLib.setPropertyString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())

    MPVLib.setPropertyDouble("sub-delay", subtitlesPreferences.defaultSubDelay.get() / 1000.0)
    MPVLib.setPropertyDouble("sub-speed", subtitlesPreferences.defaultSubSpeed.get().toDouble())
    MPVLib.setPropertyDouble("secondary-sub-delay", subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0)
  }

  private fun copyMPVConfigFiles() {
    val applicationPath = filesDir.path
    try {
      val mpvConf = fileManager.fromUri(Uri.parse(advancedPreferences.mpvConfStorageUri.get()))
        ?: error("User hasn't set any mpvConfig directory")
      if (!fileManager.exists(mpvConf)) error("Couldn't access mpv configuration directory")
      fileManager.copyDirectoryWithContent(mpvConf, fileManager.fromPath(applicationPath), true)
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf").writeText(advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf").writeText(advancedPreferences.inputConf.get())
      Log.e("PlayerActivity", "Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun copyMPVFonts() {
    try {
      val cachePath = cacheDir.path
      val fontsDir = fileManager.fromUri(Uri.parse(subtitlesPreferences.fontsFolder.get()))
        ?: error("User hasn't set any fonts directory")
      if (!fileManager.exists(fontsDir)) error("Couldn't access fonts directory")

      val destDir = fileManager.fromPath("$cachePath/fonts")
      if (!fileManager.exists(destDir)) fileManager.createDir(fileManager.fromPath(cachePath), "fonts")

      if (fileManager.findFile(destDir, "subfont.ttf") == null) {
        resources.assets.open("subfont.ttf")
          .copyTo(File("$cachePath/fonts/subfont.ttf").outputStream())
      }

      fileManager.copyDirectoryWithContent(fontsDir, destDir, false)
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

  @Suppress("NestedBlockDepth")
  private fun parsePathFromIntent(intent: Intent): String? {
    intent.getStringArrayExtra("headers")?.let { headers ->
      if (headers[0].startsWith("User-Agent", true)) MPVLib.setPropertyString("user-agent", headers[1])
      val headersString = headers.asSequence().drop(2).chunked(2).associate { it[0] to it[1] }
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }.joinToString(",")
      MPVLib.setPropertyString("http-header-fields", headersString)
    }
    return when (intent.action) {
      Intent.ACTION_VIEW -> intent.data?.let { resolveUri(it) }
      Intent.ACTION_SEND -> {
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
          resolveUri(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)
        } else {
          intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            val uri = Uri.parse(it.trim())
            if (uri.isHierarchical && !uri.isRelative) resolveUri(uri) else null
          }
        }
      }

      else -> intent.getStringExtra("uri")
    }
  }

  private fun getFileName(intent: Intent): String? {
    val uri = (intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
      val cursor = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null)
      if (cursor?.moveToFirst() == true) return cursor.getString(0).also { cursor.close() }
    }
    return uri?.lastPathSegment?.substringAfterLast("/")
  }

  private fun resolveUri(data: Uri): String? {
    val filepath = when {
      data.scheme == "file" -> data.path
      data.scheme == "content" -> openContentFd(data)
      Utils.PROTOCOLS.contains(data.scheme) -> data.toString()
      else -> null
    }

    if (filepath == null) Log.e("mpvKt", "unknown scheme: ${data.scheme}")
    return filepath
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if (!isInPictureInPictureMode) viewModel.changeVideoAspect(playerPreferences.videoAspect.get())
    }
    super.onConfigurationChanged(newConfig)
  }

  @Suppress("ReturnCount")
  fun openContentFd(uri: Uri): String? {
    if (uri.scheme != "content") return null
    val resolver = contentResolver
    Log.d(TAG, "Resolving content URI: $uri")
    val fd = try {
      val desc = resolver.openFileDescriptor(uri, "r")
      desc!!.detachFd()
    } catch (e: Exception) {
      Log.d(TAG, "Failed to open content fd: $e")
      return null
    }
    try {
      val path = File("/proc/self/fd/$fd").canonicalPath
      if (!path.startsWith("/proc") && File(path).canRead()) {
        Log.d(TAG, "Found real file path: $path")
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
    if (player.isExiting) return
    when (property) {
      "time-pos" -> viewModel.updatePlayBackPos(value.toFloat())
      "demuxer-cache-time" -> viewModel.updateReadAhead(value = value)
      "duration" -> viewModel.duration.update { value.toFloat() }
      "chapter" -> viewModel.updateChapter(value)
    }
  }

  internal fun onObserverEvent(property: String) {
    if (player.isExiting) return
    when (property) {
      "chapter-list" -> viewModel.loadChapters()
      "track-list" -> viewModel.loadTracks()
    }
  }

  internal fun onObserverEvent(property: String, value: Boolean) {
    if (player.isExiting) return
    when (property) {
      "pause" -> {
        if (value) {
          viewModel.pause()
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          viewModel.unpause()
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }

      "paused-for-cache" -> {
        viewModel.isLoading.update { value }
      }

      "seeking" -> {
        viewModel.isLoading.update { value }
      }

      "eof-reached" -> {
        if (value) endPlayback(EndPlaybackReason.PlaybackCompleted)
      }
    }
  }

  val trackId: (String) -> Int? = {
    when (it) {
      "auto" -> null
      "no" -> -1
      else -> it.toInt()
    }
  }

  internal fun onObserverEvent(property: String, value: String) {
    if (player.isExiting) return
    when (property) {
      "aid" -> trackId(value)?.let { viewModel.selectAudio(it) }
      "sid" -> trackId(value)?.let { viewModel.setSubtitle(it, viewModel.selectedSubtitles.value.second) }
      "secondary-sid" -> trackId(value)?.let { viewModel.setSubtitle(viewModel.selectedSubtitles.value.first, it) }
      "hwdec", "hwdec-current" -> viewModel.getDecoder()
    }
  }

  internal fun onObserverEvent(property: String, value: Double) {
    if (player.isExiting) return
    when (property) {
      "speed" -> viewModel.playbackSpeed.update { value.toFloat() }
    }
  }

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        getFileName(intent)?.let { fileName = it }
        viewModel.mediaTitle.update {
          val mediaTitle = MPVLib.getPropertyString("media-title")
          if (mediaTitle.isBlank() || mediaTitle.isDigitsOnly()) fileName else mediaTitle
        }
        lifecycleScope.launch(Dispatchers.IO) {
          loadVideoPlaybackState(fileName)
          if (intent.hasExtra("position")) setupIntents(intent)
        }
        setOrientation()
        viewModel.changeVideoAspect(playerPreferences.videoAspect.get())
      }

      MPVLib.mpvEventId.MPV_EVENT_SEEK -> {
        viewModel.isLoading.update { true }
      }

      MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
        player.isExiting = false
      }
    }
  }

  private suspend fun saveVideoPlaybackState(finishedPlayback: Boolean) {
    if (!::fileName.isInitialized) return
    mpvKtDatabase.videoDataDao().upsert(
      PlaybackStateEntity(
        mediaTitle = fileName,
        lastPosition = if (playerPreferences.savePositionOnQuit.get()) {
          if (finishedPlayback) 0 else player.timePos ?: 0
        } else {
          0
        },
        playbackSpeed = player.playbackSpeed ?: playerPreferences.defaultSpeed.get().toDouble(),
        sid = player.sid,
        subDelay = ((player.subDelay ?: 0.0) * 1000).toInt(),
        subSpeed = MPVLib.getPropertyDouble("sub-speed") ?: 1.0,
        secondarySid = player.secondarySid,
        secondarySubDelay = ((player.secondarySubDelay ?: 0.0) * 1000).toInt(),
        aid = player.aid,
        audioDelay = ((MPVLib.getPropertyDouble("audio-delay") ?: 0.0) * 1000).toInt(),
      ),
    )
  }

  private suspend fun loadVideoPlaybackState(mediaTitle: String) {
    val state = mpvKtDatabase.videoDataDao().getVideoDataByTitle(mediaTitle)
    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay ->
      (stateDelay ?: preferenceDelay) / 1000.0
    }
    val subDelay = getDelay(subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(audioPreferences.defaultAudioDelay.get(), state?.audioDelay)
    state?.let {
      player.sid = it.sid
      player.secondarySid = it.secondarySid
      player.aid = it.aid
      player.subDelay = subDelay
      player.secondarySubDelay = secondarySubDelay
      player.playbackSpeed = it.playbackSpeed
      MPVLib.setPropertyDouble("audio-delay", audioDelay)
    }
    player.timePos = if (playerPreferences.savePositionOnQuit.get()) state?.lastPosition ?: 0 else 0
    MPVLib.setPropertyDouble("sub-speed", state?.subSpeed ?: subtitlesPreferences.defaultSubSpeed.get().toDouble())
  }

  private fun endPlayback(reason: EndPlaybackReason, cause: String? = null) {
    Log.d(TAG, "Ending playback")
    lifecycleScope.launch(Dispatchers.IO) {
      saveVideoPlaybackState(true)
    }
    val returnIntent = Intent()
    returnIntent.putExtra("end_by", reason.value)
    cause?.let { returnIntent.putExtra("cause", cause) }
    player.timePos?.let { returnIntent.putExtra("position", it * 1000) }
    player.duration?.let { returnIntent.putExtra("duration", it * 1000) }
    setResult(RESULT_OK, returnIntent)
    if (playerPreferences.closeAfterReachingEndOfVideo.get()) {
      player.isExiting = true
      finishAndRemoveTask()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    val uri = parsePathFromIntent(intent)
    val videoUri = if (uri?.startsWith("content://") == true) {
      openContentFd(Uri.parse(uri))
    } else {
      uri
    }
    videoUri?.let { MPVLib.command(arrayOf("loadfile", it)) }
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
      if (rational in 0.42..2.38) builder.setAspectRatio(Rational(width.toInt(), height))
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
    requestedOrientation = when (playerPreferences.orientation.get()) {
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
      KeyEvent.KEYCODE_VOLUME_UP -> viewModel.changeVolumeBy(1)
      KeyEvent.KEYCODE_VOLUME_DOWN -> viewModel.changeVolumeBy(-1)
      KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.seekBy(playerPreferences.doubleTapToSeekDuration.get())
      KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.seekBy(-playerPreferences.doubleTapToSeekDuration.get())
      KeyEvent.KEYCODE_SPACE -> viewModel.pauseUnpause()
      KeyEvent.KEYCODE_MEDIA_STOP -> endPlayback(EndPlaybackReason.ExternalAction)

      // They don't have a seek animation cause that's in GestureHandler.kt :despair:
      KeyEvent.KEYCODE_MEDIA_REWIND -> viewModel.seekBy(-playerPreferences.doubleTapToSeekDuration.get())
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> viewModel.seekBy(playerPreferences.doubleTapToSeekDuration.get())

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

const val TAG = "mpvKt"
