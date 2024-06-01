package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.player.controls.LocalPlayerButtonsClickEvent

@Composable
fun ControlsButton(
  icon: ImageVector,
  title: String? = null,
  color: Color = Color.White,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  Box(
    modifier = Modifier
      .clickable(
        onClick = {
          clickEvent()
          onClick()
        },
        interactionSource = interactionSource,
        indication = rememberRipple(),
      )
      .padding(16.dp),
  ) {
    Icon(
      icon,
      title,
      tint = color,
    )
  }
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    null,
  ) { }
}
