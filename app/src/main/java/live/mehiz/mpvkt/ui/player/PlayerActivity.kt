package live.mehiz.mpvkt.ui.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.vivvvek.seeker.Seeker
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.preferences.PlayerPreferences
import org.koin.android.ext.android.inject
import java.io.File

class PlayerActivity : AppCompatActivity() {

  val viewModel: PlayerViewModel by lazy { PlayerViewModel(this) }
  val binding by lazy { PlayerLayoutBinding.inflate(this.layoutInflater) }
  val player by lazy { binding.player }
  private val playerPreferences by inject<PlayerPreferences>()

  @SuppressLint("StateFlowValueCalledInComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
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
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
      ConstraintLayout(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      ) {
        val position by viewModel.pos.collectAsState()
        val (seekbar) = createRefs()
        AnimatedVisibility(
          visible = controlsShown,
          enter = slideInVertically(initialOffsetY = { it }),
          exit = slideOutVertically(targetOffsetY = { it }),
          modifier = Modifier.constrainAs(seekbar) {
            bottom.linkTo(parent.bottom)
          }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = Utils.prettyTime(position.toInt()),
              color = Color.White,
            )
            Seeker(
              value = position,
              range = 0f..(player.duration?.toFloat() ?: 0f),
              onValueChange = {
                player.paused = true
                viewModel.updatePlayBackPos(it)
                player.timePos = it.toInt()
              },
              onValueChangeFinished = { player.paused = false },
              modifier = Modifier.weight(1f),
            )
            Text(
              text = Utils.prettyTime(player.duration ?: 0),
              color = Color.White,
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
