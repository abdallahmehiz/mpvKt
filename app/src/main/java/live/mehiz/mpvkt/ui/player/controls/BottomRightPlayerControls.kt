package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.theme.spacing

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun BottomRightPlayerControls(
  customButton: CustomButtonEntity?,
  customButtonTitle: String,
  isPipAvailable: Boolean,
  onCustomButtonClick: () -> Unit,
  onCustomButtonLongClick: () -> Unit,
  onAspectClick: () -> Unit,
  onPipClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier) {
    if (customButton != null) {
      // One node: the visible button and the gesture target are the same thing, so a screen
      // reader's activate gesture runs the button instead of hitting an empty onClick underneath.
      Surface(
        modifier =
          Modifier
            .padding(end = MaterialTheme.spacing.smaller)
            .combinedClickable(
              onClick = onCustomButtonClick,
              onLongClick = onCustomButtonLongClick,
            )
            .semantics { contentDescription = customButtonTitle },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text = customButtonTitle,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }

    if (isPipAvailable) {
      ControlsButton(
        Icons.Default.PictureInPictureAlt,
        onClick = onPipClick,
        title = stringResource(R.string.a11y_btn_pip),
      )
    }

    ControlsButton(
      Icons.Default.AspectRatio,
      onClick = onAspectClick,
      title = stringResource(R.string.a11y_btn_aspect),
    )
  }
}
