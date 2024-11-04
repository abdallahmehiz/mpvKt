package live.mehiz.mpvkt.ui.player.controls

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject

@SuppressLint("NewApi")
@Composable
fun BottomRightPlayerControls(modifier: Modifier = Modifier) {
  val viewModel = koinInject<PlayerViewModel>()
  val playerPreferences = koinInject<PlayerPreferences>()
  val aspect by playerPreferences.videoAspect.collectAsState()
  val primaryCustomButtonId by playerPreferences.primaryCustomButtonId.collectAsState()
  val customButtons by viewModel.customButtons.collectAsState()
  Row(modifier) {
    if (primaryCustomButtonId != 0 && customButtons.getButtons().isNotEmpty()) {
      val button = customButtons.getButtons().first { it.id == primaryCustomButtonId }
      Button(
        onClick = { viewModel.executeCustomButton(button) },
        modifier = Modifier.padding(end = MaterialTheme.spacing.smaller),
      ) {
        Text(text = button.title)
      }
    }

    val activity = LocalContext.current as PlayerActivity
    if (activity.isPipSupported) {
      ControlsButton(
        Icons.Default.PictureInPictureAlt,
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.enterPictureInPictureMode(activity.createPipParams())
          } else {
            activity.enterPictureInPictureMode()
          }
        },
      )
    }
    ControlsButton(
      Icons.Default.AspectRatio,
      onClick = {
        when (aspect) {
          VideoAspect.Fit -> viewModel.changeVideoAspect(VideoAspect.Stretch)
          VideoAspect.Stretch -> viewModel.changeVideoAspect(VideoAspect.Crop)
          VideoAspect.Crop -> viewModel.changeVideoAspect(VideoAspect.Fit)
        }
      },
    )
  }
}
