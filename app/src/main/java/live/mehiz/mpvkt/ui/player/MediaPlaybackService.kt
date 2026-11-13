package live.mehiz.mpvkt.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import live.mehiz.mpvkt.ui.player.manager.PlayerManager
import java.io.File

@Suppress("TooManyFunctions", "LargeClass")
class MediaPlaybackService : MediaBrowserServiceCompat() {
  companion object {
    private const val NOTIFICATION_ID = 69420
    private const val NOTIFICATION_CHANNEL_ID = "mpvkt_playback_channel"

    const val ACTION_PLAY = "live.mehiz.mpvkt.action.PLAY"
    const val ACTION_PAUSE = "live.mehiz.mpvkt.action.PAUSE"
    const val ACTION_STOP = "live.mehiz.mpvkt.action.STOP"
    const val ACTION_SKIP_FORWARD = "live.mehiz.mpvkt.action.SKIP_FORWARD"
    const val ACTION_SKIP_BACKWARD = "live.mehiz.mpvkt.action.SKIP_BACKWARD"
  }

  inner class MediaPlaybackBinder : Binder() {
    val service: MediaPlaybackService get() = this@MediaPlaybackService
  }

  private val playerPreferences = App.graph.playerPreferences
  private val gesturePreferences = App.graph.gesturePreferences
  private val audioPreferences = App.graph.audioPreferences
  private val subtitlesPreferences = App.graph.subtitlesPreferences
  private val decoderPreferences = App.graph.decoderPreferences
  private val advancedPreferences = App.graph.advancedPreferences
  private val playbackStateRepository = App.graph.playbackStateRepository
  private val fileManager = App.graph.fileManager

  private val binder = MediaPlaybackBinder()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  // not tied to [scope], a state save started right before onDestroy must still finish
  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  lateinit var mpv: MPV
    private set

  lateinit var playerManager: PlayerManager
    private set

  private val _closeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  /** Emits when playback was stopped from outside the activity (notification, media session). */
  val closeRequests = _closeRequests.asSharedFlow()

  var fileName = ""
    private set
  private var playIntent: Intent? = null
  private var pendingFile: String? = null
  private var hasSurface = false
  private var savedVid: String? = null
  private var voInUse = "gpu"
  private var isForeground = false
  private var isStopping = false

  private var paused: Boolean
    get() = mpv.prop["pause"] ?: true
    set(value) {
      mpv.prop("pause", value)
    }

  private val mediaTitle: String? get() = mpv.prop["media-title"]
  private val mediaArtist: String? get() = mpv.prop["metadata/artist"]
  private val durationMs: Long get() = (mpv.getPropertyLong("duration") ?: 0L) * 1000L
  private var positionMs: Long
    get() = (mpv.prop["time-pos"] ?: 0L) * 1000L
    set(value) {
      mpv.command("seek", (value / 1000f).toString(), "absolute")
    }

  private lateinit var mediaSession: MediaSessionCompat
  private var mediaThumbnail: Bitmap? = null

  private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
  private var audioFocusRequest: AudioFocusRequestCompat? = null
  private var restoreAudioFocus: () -> Unit = {}

  private val noisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) paused = true
    }
  }

  override fun onCreate() {
    super.onCreate()
    setupMPV()
    setupMediaSession()
    createNotificationChannel()
    ContextCompat.registerReceiver(
      this,
      noisyReceiver,
      IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    mpv.propFlow<Boolean>("pause")
      .onEach {
        updatePlaybackState()
        updateNotification()
      }
      .launchIn(scope)
    merge(mpv.propFlow<String>("media-title"), mpv.propFlow<String>("metadata/artist"))
      .onEach {
        updateMediaSessionMetadata()
        updateNotification()
      }
      .launchIn(scope)
    merge(mpv.propFlow<Long>("time-pos"), mpv.propFlow<Long>("duration"))
      .onEach { updatePlaybackState() }
      .launchIn(scope)
    mpv.eventFlow
      .onEach { (eventId, _) -> if (eventId == MPV.mpvEvent.MPV_EVENT_FILE_LOADED) onFileLoaded() }
      .launchIn(scope)
  }

  override fun onBind(intent: Intent): IBinder =
    if (intent.action == SERVICE_INTERFACE) super.onBind(intent)!! else binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mediaSession, intent)
    when (intent?.action) {
      ACTION_PLAY -> paused = false
      ACTION_PAUSE -> paused = true
      ACTION_STOP -> stopPlayback(fromOutside = true)
      ACTION_SKIP_FORWARD -> seekBy(1)
      ACTION_SKIP_BACKWARD -> seekBy(-1)
    }
    return START_NOT_STICKY
  }

  // swiped away from recents while nothing plays in the background: nobody can reach us anymore
  override fun onTaskRemoved(rootIntent: Intent?) {
    if (!isForeground) stopPlayback(fromOutside = true)
    super.onTaskRemoved(rootIntent)
  }

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: android.os.Bundle?) =
    BrowserRoot("root_id", null)

  override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
    result.sendResult(mutableListOf())
  }

  override fun onDestroy() {
    scope.cancel()
    unregisterReceiver(noisyReceiver)
    mediaSession.release()
    abandonAudioFocus()
    mpv.close()
    super.onDestroy()
  }

  // playback
  fun play(intent: Intent) {
    val path = getPlayableUri(intent) ?: return
    if (fileName.isNotBlank()) saveState()
    playIntent = intent
    if (hasSurface) mpv.command("loadfile", path) else pendingFile = path
    requestAudioFocus()
    mediaSession.isActive = true
  }

  fun stopPlayback(fromOutside: Boolean = false) {
    if (isStopping) return
    isStopping = true
    saveState()
    mediaSession.isActive = false
    if (fromOutside) _closeRequests.tryEmit(Unit)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  fun enterBackground() {
    if (isForeground || isStopping) return
    mediaThumbnail = mpv.grabThumbnail(1080)
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      createNotification(),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
    )
    isForeground = true
  }

  fun exitBackground() {
    if (!isForeground) return
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    isForeground = false
  }

  private fun seekBy(direction: Int) {
    positionMs += direction * gesturePreferences.doubleTapToSeekDuration.get() * 1000L
  }

  fun attachSurface(surface: Surface) {
    hasSurface = true
    mpv.attachSurface(surface)
    mpv.setOptionString("force-window", "yes")
    mpv.setPropertyString("vo", voInUse)
    savedVid?.let { mpv.setPropertyString("vid", it) }
    savedVid = null
    pendingFile?.let { mpv.command("loadfile", it) }
    pendingFile = null
  }

  fun resizeSurface(width: Int, height: Int) {
    mpv.setPropertyString("android-surface-size", "${width}x$height")
  }

  fun detachSurface() {
    // just to be safe
    savedVid = mpv.getPropertyString("vid")
    mpv.setPropertyString("vid", "no")
    hasSurface = false
    mpv.setPropertyString("vo", "null")
    mpv.setPropertyString("force-window", "no")
    mpv.detachSurface()
  }

  // mpv setup

  private fun setupMPV() {
    Utils.copyAssets(this)
    copyMPVScripts()
    copyMPVConfigFiles()
    // fonts can be lazily loaded
    ioScope.launch { copyMPVFonts() }

    mpv = MPV(this)
    val configDir = filesDir.path
    mpv.setConfigDir(configDir)
    mpv.setCacheDir(cacheDir.path)
    File(configDir, "mpv.conf").takeIf { it.exists() }?.let { mpv.command("load-config-file", it.path) }
    File(configDir, "input.conf").takeIf { it.exists() }?.let { mpv.command("load-input-conf", it.path) }
    initOptions()
    postInitOptions()
    // could mess up VO init before a surface is attached
    mpv.setOptionString("force-window", "no")
    audioPreferences.audioChannels.get().let { mpv.setPropertyString(it.property, it.value) }
    playerManager = PlayerManager(mpv, this, App.graph, scope)
  }

  private fun initOptions() {
    voInUse = if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu"
    mpv.setOptionString("vo", voInUse)
    mpv.setOptionString("profile", "fast")
    mpv.setOptionString("hwdec", if (decoderPreferences.tryHWDecoding.get()) "auto" else "no")

    if (decoderPreferences.useYUV420P.get()) mpv.setOptionString("vf", "format=yuv420p")

    mpv.setOptionString("msg-level", "all=" + if (advancedPreferences.verboseLogging.get()) "v" else "warn")

    mpv.setPropertyBoolean("keep-open", true)
    mpv.setPropertyBoolean("input-default-bindings", true)

    mpv.setOptionString("tls-verify", "yes")
    mpv.setOptionString("tls-ca-file", "${filesDir.path}/cacert.pem")

    // Limit demuxer cache since the defaults are too high for mobile devices
    val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
    mpv.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    mpv.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")

    val screenshotDir = android.os.Environment.getExternalStoragePublicDirectory(
      android.os.Environment.DIRECTORY_PICTURES,
    )
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

  private fun postInitOptions() {
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

  private fun setupAudioOptions() {
    mpv.setOptionString("alang", audioPreferences.preferredLanguages.get())
    mpv.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    mpv.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    mpv.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
  }

  private fun setupSubtitlesOptions() {
    mpv.setOptionString("slang", subtitlesPreferences.preferredLanguages.get())

    mpv.setOptionString("sub-fonts-dir", cacheDir.path + "/fonts/")
    mpv.setOptionString("sub-delay", (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString())
    mpv.setOptionString("sub-speed", subtitlesPreferences.defaultSubSpeed.get().toString())
    mpv.setOptionString(
      "secondary-sub-delay",
      (subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0).toString(),
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

  private fun copyMPVConfigFiles() {
    val applicationPath = filesDir.path
    try {
      val mpvConf = fileManager.fromUri(advancedPreferences.mpvConfStorageUri.get().toUri())
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
      Log.e(TAG, "Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun copyMPVScripts() {
    val scriptsDir = fileManager.createDir(fileManager.fromPath(filesDir.path), "scripts")!!
    fileManager.deleteContent(scriptsDir)
    File("$scriptsDir/mpvkt.lua")
      .also { if (!it.exists()) it.createNewFile() }
      .writeText(assets.open("mpvkt.lua").bufferedReader().use { it.readText() })
  }

  private fun copyMPVFonts() {
    try {
      val cachePath = cacheDir.path
      val fontsDir = fileManager.fromUri(subtitlesPreferences.fontsFolder.get().toUri())
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
      Log.e(TAG, "Couldn't copy fonts to application directory: ${e.message}")
    }
  }

  fun setupCustomButtons(buttons: List<CustomButtonEntity>) {
    val scriptsDir = fileManager.createDir(fileManager.fromPath(filesDir.path), "scripts")!!

    val customButtonsContent = buildString {
      appendLine("local lua_modules = mp.find_config_file('scripts')")
      appendLine("if lua_modules then")
      appendLine("package.path = package.path .. ';' .. lua_modules .. '/?.lua;' .. lua_modules .. '/?/init.lua'")
      appendLine("end")
      appendLine("local mpvkt = require 'mpvkt'")
      buttons.forEach { button ->
        appendLine("function button${button.id}()")
        appendLine(button.content)
        appendLine("end")
        appendLine("mp.register_script_message('call_button_${button.id}', button${button.id})")
        appendLine("function button${button.id}long()")
        appendLine(button.longPressContent)
        appendLine("end")
        appendLine("mp.register_script_message('call_button_${button.id}_long', button${button.id}long)")
      }
    }

    val file = File("$scriptsDir/custombuttons.lua")
      .also { if (!it.exists()) it.createNewFile() }
    file.writeText(customButtonsContent)
    mpv.command("load-script", file.absolutePath)
  }

  private fun getPlayableUri(intent: Intent): String? {
    val uri = parsePathFromIntent(intent)
    return if (uri?.startsWith("content://") == true) uri.toUri().openContentFd(this) else uri
  }

  private fun parsePathFromIntent(intent: Intent): String? = when (intent.action) {
    Intent.ACTION_VIEW -> intent.data?.resolveUri(this)

    Intent.ACTION_SEND -> {
      if (intent.hasExtra(Intent.EXTRA_STREAM)) {
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!.resolveUri(this)
      } else {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
          val uri = it.trim().toUri()
          if (uri.isHierarchical && !uri.isRelative) uri.resolveUri(this) else null
        }
      }
    }

    else -> intent.getStringExtra("uri")
  }

  private fun getFileName(intent: Intent): String {
    val uri = if (intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)!!.toUri()
    } else {
      (intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
      contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
      }
    }
    return uri?.lastPathSegment?.substringAfterLast("/") ?: uri?.path ?: ""
  }

  private fun onFileLoaded() {
    val intent = playIntent ?: return
    fileName = getFileName(intent)
    setIntentExtras(intent.extras)
    val title = mpv.getPropertyString("media-title")
    if (title.isNullOrBlank() || title.isDigitsOnly()) mpv.setPropertyString("media-title", fileName)
    val name = fileName
    ioScope.launch { loadVideoPlaybackState(name) }
  }

  private fun setIntentExtras(extras: android.os.Bundle?) {
    if (extras == null) return

    extras.getString("title")?.let { mpv.setPropertyString("force-media-title", it) }
    mpv.setPropertyInt("time-pos", extras.getInt("position", 0) / 1000)

    if (extras.containsKey("subs")) {
      val subList = Utils.getParcelableArray<Uri>(extras, "subs")
      val subsToEnable = Utils.getParcelableArray<Uri>(extras, "subs.enable")

      for (suburi in subList) {
        val subfile = suburi.resolveUri(this) ?: continue
        val flag = if (subsToEnable.any { it == suburi }) "select" else "auto"
        Log.v(TAG, "Adding subtitles from intent extras: $subfile")
        mpv.command("sub-add", subfile, flag)
      }
    }

    extras.getStringArray("headers")?.let { headers ->
      if (headers[0].startsWith("User-Agent", true)) mpv.setPropertyString("user-agent", headers[1])
      val headersString = headers.asSequence().drop(2).chunked(2).associate { it[0] to it[1] }
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }.joinToString(",")
      mpv.setPropertyString("http-header-fields", headersString)
    }
  }

  fun saveState() {
    val name = fileName
    if (name.isBlank() || !mpv.isInitialized) return
    val pos = mpv.prop["time-pos"] ?: 0
    val duration = mpv.prop["duration"] ?: 0
    val snapshot = PlaybackStateEntity(
      mediaTitle = name,
      lastPosition = if (pos < duration - 1) pos else 0,
      playbackSpeed = mpv.prop["speed"] ?: 1.0,
      sid = mpv.prop["sid"] ?: 0,
      subDelay = delayMs("sub-delay"),
      subSpeed = mpv.prop["sub-speed"] ?: 1.0,
      secondarySid = mpv.prop["secondary-sid"] ?: 0,
      secondarySubDelay = delayMs("secondary-sub-delay"),
      aid = mpv.prop["aid"] ?: 0,
      audioDelay = delayMs("audio-delay"),
    )
    ioScope.launch {
      val keepOld = !playerPreferences.savePositionOnQuit.get()
      val state = if (keepOld) {
        snapshot.copy(lastPosition = playbackStateRepository.getVideoDataByTitle(name)?.lastPosition ?: 0)
      } else {
        snapshot
      }
      Log.d(TAG, "Saving playback state")
      playbackStateRepository.upsert(state)
    }
  }

  private fun delayMs(property: String): Int = ((mpv.prop[property] ?: 0.0) * 1000).toInt()

  private suspend fun loadVideoPlaybackState(mediaTitle: String) {
    if (mediaTitle.isBlank()) return
    val state = playbackStateRepository.getVideoDataByTitle(mediaTitle)
    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay ->
      (stateDelay ?: preferenceDelay) / 1000.0
    }
    val subDelay = getDelay(subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(audioPreferences.defaultAudioDelay.get(), state?.audioDelay)
    state?.let {
      mpv.prop("sid", it.sid)
      mpv.prop("secondary-sid", it.secondarySid)
      mpv.prop("aid", it.aid)
      mpv.prop("sub-delay", subDelay)
      mpv.prop("secondary-sub-delay", secondarySubDelay)
      mpv.prop("speed", it.playbackSpeed)
      mpv.prop("audio-delay", audioDelay)
    }
    if (playerPreferences.savePositionOnQuit.get()) {
      state?.lastPosition?.let { if (it != 0) mpv.prop("time-pos", it) }
    }
    mpv.prop("sub-speed", state?.subSpeed ?: subtitlesPreferences.defaultSubSpeed.get().toDouble())
  }

  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    when (it) {
      AudioManager.AUDIOFOCUS_LOSS,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
      -> {
        val oldRestore = restoreAudioFocus
        val wasPaused = paused
        paused = true
        restoreAudioFocus = {
          oldRestore()
          if (!wasPaused) paused = false
        }
      }

      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        mpv.command("multiply", "volume", "0.5")
        restoreAudioFocus = { mpv.command("multiply", "volume", "2") }
      }

      AudioManager.AUDIOFOCUS_GAIN -> {
        restoreAudioFocus()
        restoreAudioFocus = {}
      }
    }
  }

  private fun requestAudioFocus() {
    if (audioFocusRequest != null) return
    val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
      .setAudioAttributes(
        AudioAttributesCompat.Builder()
          .setUsage(AudioAttributesCompat.USAGE_MEDIA)
          .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
          .build(),
      )
      .setOnAudioFocusChangeListener(audioFocusChangeListener)
      .build()
    if (AudioManagerCompat.requestAudioFocus(audioManager, request) != AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
      audioFocusRequest = request
    } else {
      Log.d(TAG, "didn't get audio focus")
    }
  }

  private fun abandonAudioFocus() {
    audioFocusRequest?.let { AudioManagerCompat.abandonAudioFocusRequest(audioManager, it) }
    audioFocusRequest = null
  }

  private fun runGesture(gesture: SingleActionGesture, direction: Int, key: CustomKeyCodes) {
    when (gesture) {
      SingleActionGesture.None -> {}
      SingleActionGesture.Seek -> if (direction != 0) seekBy(direction)
      SingleActionGesture.PlayPause -> paused = !paused
      SingleActionGesture.Custom -> mpv.command("keypress", key.keyCode)
    }
  }

  // play and pause are separate session calls, so PlayPause sets instead of toggling
  private fun playOrPause(pause: Boolean) {
    val gesture = gesturePreferences.mediaPlayGesture.get()
    if (gesture == SingleActionGesture.PlayPause) paused = pause else runGesture(gesture, 0, CustomKeyCodes.MediaPlay)
  }

  private fun setupMediaSession() {
    mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
      setCallback(
        object : MediaSessionCompat.Callback() {
          override fun onPlay() = playOrPause(pause = false)
          override fun onPause() = playOrPause(pause = true)

          override fun onSkipToPrevious() =
            runGesture(gesturePreferences.mediaPreviousGesture.get(), -1, CustomKeyCodes.MediaPrevious)
          override fun onSkipToNext() =
            runGesture(gesturePreferences.mediaNextGesture.get(), 1, CustomKeyCodes.MediaNext)

          override fun onStop() = stopPlayback(fromOutside = true)
          override fun onSeekTo(pos: Long) {
            positionMs = pos
          }
        },
      )
      setSessionToken(sessionToken)
      setPlaybackState(
        PlaybackStateCompat.Builder()
          .setActions(availableActions)
          .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
          .build(),
      )
    }
  }

  private val availableActions: Long = PlaybackStateCompat.ACTION_PLAY or
    PlaybackStateCompat.ACTION_PAUSE or
    PlaybackStateCompat.ACTION_PLAY_PAUSE or
    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
    PlaybackStateCompat.ACTION_STOP or
    PlaybackStateCompat.ACTION_SEEK_TO

  private fun updateMediaSessionMetadata() {
    val metadataBuilder = MediaMetadataCompat.Builder()
      .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaTitle)
      .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaArtist)
      .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
      .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mediaTitle)
      .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaTitle.hashCode().toString())
    mediaThumbnail?.let {
      metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
      metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
    }
    mediaSession.setMetadata(metadataBuilder.build())
  }

  private fun updatePlaybackState() {
    mediaSession.setPlaybackState(
      PlaybackStateCompat.Builder()
        .setActions(availableActions)
        .setState(
          if (paused) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
          positionMs,
          1.0f,
        )
        .build(),
    )
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      NOTIFICATION_CHANNEL_ID,
      getString(R.string.notification_channel_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = getString(R.string.notification_channel_description)
      setShowBadge(false)
      enableLights(false)
      enableVibration(false)
    }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
    this,
    requestCode,
    Intent(this, MediaPlaybackService::class.java).setAction(action),
    PendingIntent.FLAG_IMMUTABLE,
  )

  private fun createNotification(): Notification {
    val openPlayer = PendingIntent.getActivity(
      this,
      0,
      Intent(this, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle(mediaTitle)
      .setContentText(mediaArtist?.ifBlank { getString(R.string.notification_playing) })
      .setSmallIcon(R.drawable.ic_launcher_foreground_monochrome)
      .setLargeIcon(mediaThumbnail)
      .setContentIntent(openPlayer)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOnlyAlertOnce(true)
      .setOngoing(!paused)
      .addAction(
        R.drawable.baseline_fast_rewind_24,
        getString(R.string.notification_rewind),
        serviceIntent(ACTION_SKIP_BACKWARD, 4),
      )
      .addAction(
        if (paused) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24,
        getString(if (paused) R.string.notification_play else R.string.notification_pause),
        if (paused) serviceIntent(ACTION_PLAY, 1) else serviceIntent(ACTION_PAUSE, 2),
      )
      .addAction(
        R.drawable.baseline_fast_forward_24,
        getString(R.string.notification_forward),
        serviceIntent(ACTION_SKIP_FORWARD, 3),
      )
      .addAction(
        R.drawable.sharp_shadow_24,
        getString(R.string.notification_stop),
        serviceIntent(ACTION_STOP, 5),
      )
      .setStyle(
        androidx.media.app.NotificationCompat.MediaStyle()
          .setMediaSession(mediaSession.sessionToken)
          .setShowActionsInCompactView(0, 1, 2),
      )
      .setColorized(true)
      .setColor(Color.BLUE)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun updateNotification() {
    if (!isForeground) return // only play when activity is not on the foreground
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification())
  }
}
