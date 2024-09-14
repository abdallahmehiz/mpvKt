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
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.controls.PlayerControls
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
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val fileManager: FileManager by inject()

  private var fileName = ""

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
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    setupMPV()
    setupAudio()
    getPlayableUri(intent)?.let { player.playFile(it) }
    setIntentExtras(intent.extras)
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

  private fun getPlayableUri(intent: Intent): String? {
    val uri = parsePathFromIntent(intent)
    return if (uri?.startsWith("content://") == true) Uri.parse(uri).openContentFd(this) else uri
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
    saveVideoPlaybackState(fileName)
    super.onPause()
  }

  override fun finish() {
    setReturnIntent()
    super.finish()
  }

  override fun onStop() {
    viewModel.pause()
    saveVideoPlaybackState(fileName)
    player.isExiting = true
    window.attributes.screenBrightness.let {
      if (playerPreferences.rememberBrightness.get() && it != -1f) {
        playerPreferences.defaultBrightness.set(it)
      }
    }
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
    super.onStart()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setPictureInPictureParams(createPipParams())
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = if (playerPreferences.drawOverDisplayCutout.get()) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
      }
    }

    if (playerPreferences.rememberBrightness.get()) {
      playerPreferences.defaultBrightness.get().let {
        if (it != -1f) viewModel.changeBrightnessTo(it)
      }
    }
  }

  private fun copyMPVAssets() {
    Utils.copyAssets(this@PlayerActivity)
    copyMPVConfigFiles()
    // fonts can be lazily loaded
    lifecycleScope.launch(Dispatchers.IO) {
      copyMPVFonts()
    }
  }

  private fun setupMPV() {
    copyMPVAssets()
    player.initialize(filesDir.path, cacheDir.path)
    MPVLib.addObserver(playerObserver)
  }

  private fun setupAudio() {
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

  private fun copyMPVConfigFiles() {
    val applicationPath = filesDir.path
    try {
      val mpvConf = fileManager.fromUri(Uri.parse(advancedPreferences.mpvConfStorageUri.get()))
        ?: error("User hasn't set any mpvConfig directory")
      if (!fileManager.exists(mpvConf)) error("Couldn't access mpv configuration directory")
      fileManager.copyDirectoryWithContent(mpvConf, fileManager.fromPath(applicationPath), true)
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(advancedPreferences.inputConf.get())
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

  override fun onResume() {
    super.onResume()

    viewModel.currentVolume.update {
      audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).also {
        if (it < viewModel.maxVolume) viewModel.changeMPVVolumeTo(100)
      }
    }
    viewModel.currentBrightness.update { window.attributes.screenBrightness }
  }

  private fun setIntentExtras(extras: Bundle?) {
    if (extras == null) return

    extras.getString("title")?.let {
      viewModel.mediaTitle.update { _ -> it }
      MPVLib.setPropertyString("force-media-title", it)
    }
    player.timePos = extras.getInt("position", 0) / 1000

    extras.getStringArray("headers")?.let { headers ->
      if (headers[0].startsWith("User-Agent", true)) MPVLib.setPropertyString("user-agent", headers[1])
      val headersString = headers.asSequence().drop(2).chunked(2).associate { it[0] to it[1] }
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }.joinToString(",")
      MPVLib.setPropertyString("http-header-fields", headersString)
    }
  }

  @Suppress("NestedBlockDepth")
  private fun parsePathFromIntent(intent: Intent): String? {
    return when (intent.action) {
      Intent.ACTION_VIEW -> intent.data?.resolveUri(this)
      Intent.ACTION_SEND -> {
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
          intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!.resolveUri(this)
        } else {
          intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            val uri = Uri.parse(it.trim())
            if (uri.isHierarchical && !uri.isRelative) uri.resolveUri(this) else null
          }
        }
      }

      else -> intent.getStringExtra("uri")
    }
  }

  private fun getFileName(intent: Intent): String {
    val uri = if (intent.type == "text/plain") {
      Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
    } else {
      (intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
      val cursor = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null)
      if (cursor?.moveToFirst() == true) return cursor.getString(0).also { cursor.close() }
    }
    return uri?.lastPathSegment?.substringAfterLast("/") ?: uri?.path ?: ""
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if (!isInPictureInPictureMode) {
        viewModel.changeVideoAspect(playerPreferences.videoAspect.get())
      } else {
        viewModel.hideControls()
      }
    }
    super.onConfigurationChanged(newConfig)
  }

  // a bunch of observers
  internal fun onObserverEvent(property: String, value: Long) {
    if (player.isExiting) return
    when (property) {
      "time-pos" -> viewModel.updatePlayBackPos(value.toFloat())
      "demuxer-cache-time" -> viewModel.updateReadAhead(value = value)
      "volume" -> viewModel.setMPVVolume(value.toInt())
      "volume-max" -> viewModel.volumeBoostCap = value.toInt() - 100
      "chapter" -> viewModel.updateChapter(value)
      "duration" -> viewModel.duration.update { value.toFloat() }
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
        if (value && playerPreferences.closeAfterReachingEndOfVideo.get()) {
          finishAndRemoveTask()
        }
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
      "aid" -> trackId(value)?.let { viewModel.updateAudio(it) }
      "sid" -> trackId(value)?.let { viewModel.updateSubtitle(it, viewModel.selectedSubtitles.value.second) }
      "secondary-sid" -> trackId(value)?.let { viewModel.updateSubtitle(viewModel.selectedSubtitles.value.first, it) }
      "hwdec", "hwdec-current" -> viewModel.getDecoder()
    }
  }

  @SuppressLint("NewApi")
  internal fun onObserverEvent(property: String, value: Double) {
    if (player.isExiting) return
    when (property) {
      "speed" -> {
        if (viewModel.sheetShown.value == Sheets.PlaybackSpeed) return
        viewModel.playbackSpeed.update { value.toFloat() }
      }
      "video-params/aspect" -> if (isPipSupported) createPipParams()
    }
  }

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        fileName = getFileName(intent)
        viewModel.mediaTitle.update {
          val mediaTitle = MPVLib.getPropertyString("media-title")
          if (mediaTitle.isBlank() || mediaTitle.isDigitsOnly()) fileName else mediaTitle
        }
        lifecycleScope.launch(Dispatchers.IO) {
          loadVideoPlaybackState(fileName)
        }
        setOrientation()
        viewModel.changeVideoAspect(playerPreferences.videoAspect.get())
      }

      MPVLib.mpvEventId.MPV_EVENT_SEEK -> viewModel.isLoading.update { true }
      MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> player.isExiting = false
    }
  }

  private fun saveVideoPlaybackState(mediaTitle: String) {
    if (mediaTitle.isBlank()) return
    lifecycleScope.launch(Dispatchers.IO) {
      val oldState = mpvKtDatabase.videoDataDao().getVideoDataByTitle(fileName)
      Log.d(TAG, "Saving playback state")
      mpvKtDatabase.videoDataDao().upsert(
        PlaybackStateEntity(
          mediaTitle = mediaTitle,
          lastPosition = if (playerPreferences.savePositionOnQuit.get()) {
            if ((player.timePos ?: 0) < (player.duration ?: 0) - 1) player.timePos ?: 0 else 0
          } else {
            oldState?.lastPosition ?: 0
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
  }

  private suspend fun loadVideoPlaybackState(mediaTitle: String) {
    if (mediaTitle.isBlank()) return
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
    if (playerPreferences.savePositionOnQuit.get()) {
      state?.lastPosition?.let { if (it != 0) player.timePos = it }
    }
    MPVLib.setPropertyDouble("sub-speed", state?.subSpeed ?: subtitlesPreferences.defaultSubSpeed.get().toDouble())
  }

  private fun setReturnIntent() {
    Log.d(TAG, "setting return intent")
    setResult(
      RESULT_OK,
      Intent().apply {
        player.timePos?.let { putExtra("position", it * 1000) }
        player.duration?.let { putExtra("duration", it * 1000) }
      },
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    getPlayableUri(intent)?.let { MPVLib.command(arrayOf("loadfile", it)) }
    setIntent(intent)
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
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
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
        viewModel.displayVolumeSlider()
      }
      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        viewModel.changeVolumeBy(-1)
        viewModel.displayVolumeSlider()
      }
      KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.seekBy(playerPreferences.doubleTapToSeekDuration.get())
      KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.seekBy(-playerPreferences.doubleTapToSeekDuration.get())
      KeyEvent.KEYCODE_SPACE -> viewModel.pauseUnpause()
      KeyEvent.KEYCODE_MEDIA_STOP -> finishAndRemoveTask()

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
