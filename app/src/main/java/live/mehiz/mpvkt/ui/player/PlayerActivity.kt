package live.mehiz.mpvkt.ui.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.ui.player.controls.PlayerControls
import live.mehiz.mpvkt.ui.theme.MpvKtTheme

@Suppress("TooManyFunctions")
class PlayerActivity : AppCompatActivity() {

  private var service: MediaPlaybackService? = null
  private lateinit var player: MPVView
  private val mpv get() = player.mpv
  private val viewModel: PlayerViewModel by viewModels {
    PlayerViewModelFactory(service!!.playerManager, initialBrightness())
  }
  private val playerManager get() = service!!.playerManager

  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  private val playerPreferences = App.graph.playerPreferences

  private var loadIntentOnConnect = false
  private var pipRect: android.graphics.Rect? = null
  val isPipSupported by lazy { packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) }
  private var pipReceiver: BroadcastReceiver? = null

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
      val service = (binder as MediaPlaybackService.MediaPlaybackBinder).service
      this@PlayerActivity.service = service
      player = MPVView(this@PlayerActivity, service)
      if (loadIntentOnConnect) service.play(intent)
      loadIntentOnConnect = false
      onPlayerReady(service)
    }

    // only on a service crash; the activity has nothing to show without it
    override fun onServiceDisconnected(name: ComponentName?) {
      service = null
      finish()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    loadIntentOnConnect = savedInstanceState == null
    val serviceIntent = Intent(this, MediaPlaybackService::class.java)
    startService(serviceIntent)
    bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
  }

  private fun onPlayerReady(service: MediaPlaybackService) {
    setOrientation()
    setContent {
      MpvKtTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          AndroidView(
            factory = { player },
            modifier = Modifier.onGloballyPositioned {
              pipRect = it.boundsInWindow().toAndroidRect()
            },
          )
          PlayerControls(
            viewModel = viewModel,
            onBackPress = ::finish,
          )
        }
      }
    }

    viewModel.eventFlow
      .onEach { event ->
        when (event) {
          is PlayerViewModel.Event.SetupCustomButtons -> service.setupCustomButtons(event.buttons)

          is PlayerViewModel.Event.ChangeBrightness -> changeBrightnessTo(event.brightness)

          is PlayerViewModel.Event.ApplyVideoAspect -> service.playerManager.video.setAspect(
            event.aspect,
            displayRatio(),
          )

          is PlayerViewModel.Event.SetControls -> setControls(event.visible)

          is PlayerViewModel.Event.SoftwareKeyboard -> setSoftwareKeyboard(event.action)

          PlayerViewModel.Event.CycleRotation -> cycleScreenRotations()
        }
      }
      .launchIn(lifecycleScope)

    service.closeRequests
      .onEach { finishAndRemoveTask() }
      .launchIn(lifecycleScope)

    observePlayer()
    restoreBrightness()
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      service.exitBackground()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported) {
        setPictureInPictureParams(createPipParams())
      }
    }
  }

  // replaces the mpv observer: state comes in as flows, collected only while the UI is visible
  @SuppressLint("NewApi")
  private fun observePlayer() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        mpv.propFlow<Boolean>("pause")
          .filterNotNull()
          .onEach {
            if (it) {
              window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
              window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
          }
          .launchIn(this)

        mpv.propFlow<Boolean>("eof-reached")
          .filter { it == true && playerPreferences.closeAfterReachingEndOfVideo.get() }
          .onEach { finishAndRemoveTask() }
          .launchIn(this)

        mpv.propFlow<Double>("video-params/aspect")
          .filterNotNull()
          .filter { isPipSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O }
          .onEach { setPictureInPictureParams(createPipParams()) }
          .launchIn(this)

        mpv.eventFlow
          .filter { (eventId, _) -> eventId == MPV.mpvEvent.MPV_EVENT_FILE_LOADED }
          .onEach {
            setOrientation()
            viewModel.changeVideoAspect(playerManager.video.aspect.value)
          }
          .launchIn(this)
      }
    }
  }

  private val showStatusBar = playerPreferences.showSystemStatusBar.get()
  private fun setControls(visible: Boolean) {
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (visible) {
      if (showStatusBar) insetsController.show(WindowInsetsCompat.Type.statusBars())
    } else {
      insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
  }

  private fun initialBrightness(): Float = runCatching {
    Settings.System.getFloat(contentResolver, Settings.System.SCREEN_BRIGHTNESS).normalize(0f, 255f, 0f, 1f)
  }.getOrElse { 0f }

  @Suppress("DEPRECATION")
  private fun displayRatio(): Double {
    val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
    return metrics.widthPixels / metrics.heightPixels.toDouble()
  }

  @Suppress("DEPRECATION")
  private fun setSoftwareKeyboard(action: PlayerViewModel.KeyboardAction) {
    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    val show = when (action) {
      PlayerViewModel.KeyboardAction.Show -> true
      PlayerViewModel.KeyboardAction.Hide -> false
      PlayerViewModel.KeyboardAction.Toggle -> !imm.isActive
    }
    imm.toggleSoftInput(if (show) InputMethodManager.SHOW_FORCED else InputMethodManager.SHOW_IMPLICIT, 0)
  }

  private fun changeBrightnessTo(brightness: Float) {
    window.attributes = window.attributes.apply {
      screenBrightness = brightness
    }
  }

  private fun restoreBrightness() {
    if (!playerPreferences.rememberBrightness.get()) return
    playerPreferences.defaultBrightness.get().let {
      if (it != -1f) viewModel.changeBrightnessTo(it)
    }
  }

  private fun cycleScreenRotations() {
    requestedOrientation = when (requestedOrientation) {
      ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
      -> {
        playerPreferences.orientation.set(PlayerOrientation.SensorPortrait)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      }

      else -> {
        playerPreferences.orientation.set(PlayerOrientation.SensorLandscape)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }
  }

  override fun onDestroy() {
    Log.d(TAG, "Exiting")
    if (isFinishing) service?.stopPlayback()
    unbindService(serviceConnection)
    service = null
    super.onDestroy()
  }

  override fun finish() {
    setReturnIntent()
    super.finish()
  }

  override fun onStart() {
    super.onStart()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility =
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

    service?.exitBackground() ?: return
    restoreBrightness()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported) {
      setPictureInPictureParams(createPipParams())
    }
  }

  override fun onStop() {
    window.attributes.screenBrightness.let {
      if (playerPreferences.rememberBrightness.get() && it != -1f) {
        playerPreferences.defaultBrightness.set(it)
      }
    }
    service?.let { service ->
      service.saveState()
      if (!isFinishing) {
        if (playerPreferences.automaticBackgroundPlayback.get()) service.enterBackground() else viewModel.pause()
      }
    }
    super.onStop()
  }

  private val shouldEnterPip: Boolean
    get() = service != null &&
      isPipSupported &&
      !playerManager.playback.paused.value &&
      playerPreferences.automaticallyEnterPip.get()

  @SuppressLint("NewApi")
  override fun onUserLeaveHint() {
    if (shouldEnterPip) enterPictureInPictureMode()
    super.onUserLeaveHint()
  }

  @SuppressLint("NewApi")
  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (shouldEnterPip) {
      if (viewModel.sheetShown.value == Sheets.None && viewModel.panelShown.value == Panels.None) {
        enterPictureInPictureMode()
      }
    } else {
      @Suppress("DEPRECATION")
      super.onBackPressed()
    }
  }

  override fun onResume() {
    super.onResume()
    if (service == null) return
    playerManager.audio.refreshStreamVolume()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    if (service != null) {
      if (!isInPictureInPictureMode) {
        viewModel.changeVideoAspect(playerManager.video.aspect.value)
      } else {
        viewModel.hideControls()
      }
    }
    super.onConfigurationChanged(newConfig)
  }

  private fun setReturnIntent() {
    if (service == null) return
    Log.d(TAG, "setting return intent")
    setResult(
      RESULT_OK,
      Intent(RESULT_INTENT).apply {
        putExtra("position", playerManager.playback.position.value * 1000)
        putExtra("duration", playerManager.playback.duration.value * 1000)
      },
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    service?.play(intent)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun createPipParams(): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      builder.setTitle(mpv.getPropertyString("media-title") ?: service?.fileName)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val autoEnter = playerPreferences.automaticallyEnterPip.get()
      val playing = !playerManager.playback.paused.value
      builder.setAutoEnterEnabled(playing && autoEnter)
      builder.setSeamlessResizeEnabled(playing && autoEnter)
    }
    builder.setActions(createPipActions(this, playerManager.playback.paused.value))
    builder.setSourceRectHint(pipRect)
    mpv.getPropertyInt("video-params/h")?.let { height ->
      val width = height * (player.getVideoOutAspect() ?: return@let)
      val rational = Rational(height, width.toInt()).toFloat()
      if (rational in 0.42..2.38) builder.setAspectRatio(Rational(width.toInt(), height))
    }
    return builder.build()
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
    if (!isInPictureInPictureMode) {
      pipReceiver?.let {
        unregisterReceiver(it)
        pipReceiver = null
      }
      super.onPictureInPictureModeChanged(false, newConfig)
      return
    }
    if (service == null) {
      super.onPictureInPictureModeChanged(true, newConfig)
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
          PIP_FF -> viewModel.handleRightDoubleTap()
          PIP_FR -> viewModel.handleLeftDoubleTap()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          setPictureInPictureParams(createPipParams())
        }
      }
    }
    ContextCompat.registerReceiver(
      this,
      pipReceiver,
      IntentFilter(PIP_INTENTS_FILTER),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    super.onPictureInPictureModeChanged(true, newConfig)
  }

  private fun setOrientation() {
    requestedOrientation = when (playerPreferences.orientation.get()) {
      PlayerOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_SENSOR

      PlayerOrientation.Video -> if ((player.getVideoOutAspect() ?: 0.0) > 1.0) {
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
    if (service == null) return super.onKeyDown(keyCode, event)
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> {
        viewModel.changeVolumeBy(1)
        viewModel.displayVolumeSlider()
      }

      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        viewModel.changeVolumeBy(-1)
        viewModel.displayVolumeSlider()
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.handleLeftDoubleTap()

      KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.handleRightDoubleTap()

      KeyEvent.KEYCODE_SPACE -> viewModel.pauseUnpause()

      KeyEvent.KEYCODE_MEDIA_STOP -> finishAndRemoveTask()

      KeyEvent.KEYCODE_MEDIA_REWIND -> viewModel.handleLeftDoubleTap()

      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> viewModel.handleRightDoubleTap()

      // other keys should be bound by the user in input.conf ig
      else -> {
        event?.let { player.onKey(it) }
        super.onKeyDown(keyCode, event)
      }
    }
    return true
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
    (service != null && event != null && player.onKey(event)) || super.onKeyUp(keyCode, event)

  companion object {
    // action of result intent
    private const val RESULT_INTENT = "live.mehiz.mpvkt.ui.player.PlayerActivity.result"
  }
}

const val TAG = "mpvKt"
