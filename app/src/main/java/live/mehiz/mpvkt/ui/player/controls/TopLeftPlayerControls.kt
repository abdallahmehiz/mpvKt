package live.mehiz.mpvkt.ui.player.controls

import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import org.koin.compose.koinInject

@Composable
fun TopLeftPlayerControls(modifier: Modifier = Modifier) {
  val viewModel = koinInject<PlayerViewModel>()
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val activity = LocalContext.current as Activity
    ControlsButton(
      icon = Icons.AutoMirrored.Default.ArrowBack,
      onClick = { activity.finish() },
    )
    val mediaTitle by viewModel.mediaTitle.collectAsState()
    Text(
      mediaTitle,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}
