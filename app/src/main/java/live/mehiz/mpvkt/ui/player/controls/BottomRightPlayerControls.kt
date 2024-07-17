package live.mehiz.mpvkt.ui.player.controls

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import org.koin.compose.koinInject

@Composable
fun BottomRightPlayerControls(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val aspect by playerPreferences.videoAspect.collectAsState()
  Row(modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val activity = LocalContext.current as PlayerActivity
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
