package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.player.controls.LocalPlayerButtonsClickEvent
import live.mehiz.mpvkt.ui.theme.spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color = Color.White,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  Box(
    modifier = modifier
      .clip(CircleShape)
      .combinedClickable(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = {
          clickEvent()
          onLongClick()
        },
        interactionSource = interactionSource,
        indication = rememberRipple(),
      )
      .padding(MaterialTheme.spacing.medium),
  ) {
    Icon(
      icon,
      title,
      tint = color,
      modifier = Modifier.size(20.dp),
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  color: Color = Color.White,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  Box(
    modifier = modifier
      .clip(CircleShape)
      .combinedClickable(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = {
          clickEvent()
          onLongClick()
        },
        interactionSource = interactionSource,
        indication = rememberRipple(),
      )
      .padding(MaterialTheme.spacing.medium),
  ) {
    Text(
      text,
      color = color,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  icon: Painter,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color = Color.White,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  Box(
    modifier = modifier
      .clip(CircleShape)
      .combinedClickable(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = {
          clickEvent()
          onLongClick()
        },
        interactionSource = interactionSource,
        indication = rememberRipple(),
      )
      .padding(MaterialTheme.spacing.medium),
  ) {
    Icon(
      icon,
      title,
      tint = color,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    onClick = {},
  )
}
