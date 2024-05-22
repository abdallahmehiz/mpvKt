package live.mehiz.mpvkt.ui.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.controls.SeekbarWithTimers
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import org.koin.android.ext.android.inject
import java.io.File

class PlayerActivity : AppCompatActivity() {

  private val viewModel: PlayerViewModel by lazy { PlayerViewModel(this) }
  val binding by lazy { PlayerLayoutBinding.inflate(this.layoutInflater) }
  val player by lazy { binding.player }
  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  private val playerPreferences by inject<PlayerPreferences>()

  override fun onCreate(savedInstanceState: Bundle?) {
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    super.onCreate(savedInstanceState)
    if (intent.extras?.getString("uri")?.isNotBlank() == true) onDestroy()
    setContentView(binding.root)
    player.initialize(
      applicationContext.filesDir.path,
      applicationContext.cacheDir.path,
      "v",
    )
    player.addObserver(PlayerObserver(this))
    val uri = parsePathFromIntent(intent)
    val videoUri = if (uri?.startsWith("content://") == true) {
      openContentFd(Uri.parse(uri))
    } else {
      uri
    }
    player.playFile(videoUri!!)
    setOrientation()
    binding.controls.setContent {
      MpvKtTheme {
        val controlsShown by viewModel.controlsShown.collectAsState()
        val seekBarShown by viewModel.seekBarShown.collectAsState()
        Row(modifier = Modifier.fillMaxSize()) {
          repeat(2) { index ->
            var seekAmount by remember { mutableIntStateOf(0) }
            val alpha by animateFloatAsState(
              if (seekAmount != 0) 0.5f else 0f,
              label = "seekingAlpha",
            )
            LaunchedEffect(seekAmount) {
              delay(600)
              seekAmount = 0
              viewModel.hideSeekBar()
            }
            val interactionSource = remember { MutableInteractionSource() }
            val doubleTapToPause by playerPreferences.doubleTapToPause.collectAsState()
            val doubleTapToSeek by playerPreferences.doubleTapToSeek.collectAsState()
            val doubleTapToSeekDuration by playerPreferences.doubleTapToSeekDuration.collectAsState()
            Box(
              modifier = Modifier
                  .weight(0.5f)
                  .fillMaxHeight()
                  .graphicsLayer(alpha = alpha)
                  .pointerInput(Unit) {
                      detectTapGestures(
                          onTap = {
                              if (controlsShown) viewModel.hideControls()
                              else viewModel.showControls()
                          },
                          onDoubleTap = {
                              if (doubleTapToPause) {
                                  viewModel.pauseUnpause()
                                  return@detectTapGestures
                              }
                              if (!doubleTapToSeek) return@detectTapGestures
                              val position = viewModel.pos.value.toInt()
                              // Don't seek backwards if we're on 0:00 or forward if we're at the end
                              if (((player.duration ?: 0) == position && index == 1) || (position == 0 && index == 0)) {
                                  return@detectTapGestures
                              }
                              val seekDuration = if (index == 0) {
                                  -doubleTapToSeekDuration
                              } else {
                                  doubleTapToSeekDuration
                              }
                              seekBy(seekDuration)
                              seekAmount += seekDuration
                              viewModel.showSeekBar()
                          },
                          onPress = {
                              val press = PressInteraction.Press(it)
                              interactionSource.emit(press)
                              tryAwaitRelease()
                              interactionSource.emit(PressInteraction.Release(press))
                          },
                      )
                  }
                  .clip(if (index == 0) LeftSideOvalShape else RightSideOvalShape)
                  .background(MaterialTheme.colorScheme.primary)
                  .indication(
                      interactionSource,
                      rememberRipple(color = MaterialTheme.colorScheme.secondary),
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                (if (index == 1) "+" else "") + "${seekAmount}s",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
              )
            }
          }
        }
        val paused by viewModel.paused.collectAsState()
        var isSeeking by remember { mutableStateOf(false) }
        LaunchedEffect(controlsShown, paused, isSeeking) {
          if (controlsShown && !paused && !isSeeking) {
            delay(3_000)
            viewModel.hideControls()
          }
        }
        val transparentOverlay by animateColorAsState(
            Color.Black.copy(if (controlsShown) 0.2f else 0f),
            label = "",
        )
        ConstraintLayout(
          modifier = Modifier
              .fillMaxSize()
              .background(transparentOverlay)
              .padding(horizontal = 8.dp),
        ) {
          val position by viewModel.pos.collectAsState()
          val (seekbar, playerPauseButton) = createRefs()
          AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.constrainAs(playerPauseButton) {
              end.linkTo(parent.absoluteRight)
              start.linkTo(parent.absoluteLeft)
              top.linkTo(parent.top)
              bottom.linkTo(seekbar.top)
            },
          ) {
            val icon = if (!paused) Icons.Default.Pause else Icons.Default.PlayArrow
            val interaction = remember { MutableInteractionSource() }
            Icon(
              modifier = Modifier
                  .size(96.dp)
                  .clip(CircleShape)
                  .clickable(
                      interaction,
                      rememberRipple(color = MaterialTheme.colorScheme.onBackground),
                  ) { viewModel.pauseUnpause() },
              imageVector = icon,
              contentDescription = null,
              tint = Color.White,
            )
          }
          AnimatedVisibility(
            visible = controlsShown || seekBarShown,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.constrainAs(seekbar) {
              bottom.linkTo(parent.bottom, 16.dp)
            },
          ) {
            val invertDuration by playerPreferences.invertDuration.collectAsState()
            val readAhead by viewModel.readAhead.collectAsState()
            SeekbarWithTimers(
              position = position,
              duration = viewModel.duration,
              readAheadValue = readAhead,
              onValueChange = {
                isSeeking = true
                player.paused = true
                viewModel.updatePlayBackPos(it)
                player.timePos = it.toInt()
              },
              onValueChangeFinished = {
                if (!viewModel.paused.value) player.paused = false
                isSeeking = false
              },
              timersInverted = Pair(false, invertDuration),
              durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
              positionTimerOnClick = {},
            )
          }
        }
      }
    }
  }

  override fun onDestroy() {
    MPVLib.destroy()
    super.onDestroy()
  }

  fun seekBy(offset: Int) {
    player.timePos = player.timePos?.plus(offset)
  }

  fun seekTo(position: Int) {
    if (position < 0) return
    if (position > (player.duration ?: 0)) return
    player.timePos = position
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

  private fun openContentFd(uri: Uri): String? {
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
      "time-pos" -> {
        viewModel.updatePlayBackPos(value.toFloat())
      }

      "demuxer-cache-time" -> {
        viewModel.updateReadAhead(value = value)
      }
    }
  }

  internal fun onObserverEvent(property: String) {

  }

  internal fun onObserverEvent(property: String, value: Boolean) {

  }

  internal fun onObserverEvent(property: String, value: String) {

  }

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        setOrientation()
        viewModel.duration = player.duration!!.toFloat()
      }

      MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
        onDestroy()
      }
    }
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
}
