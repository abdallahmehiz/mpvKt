package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerUpdate(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black.copy(0.4f))
      .padding(vertical = 8.dp, horizontal = 16.dp)
      .animateContentSize(),
    contentAlignment = Alignment.Center,
  ) { content() }
}

@Composable
fun TextPlayerUpdate(
  text: String,
  modifier: Modifier = Modifier
) {
  PlayerUpdate(modifier) {
    Text(text)
  }
}

@Composable
fun DoubleSpeedPlayerUpdate(
  modifier: Modifier = Modifier
) {
  PlayerUpdate(modifier) {
    Row(
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        "2x",
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
      )
      Icon(
        Icons.Filled.DoubleArrow,
        null,
      )
    }
  }
}

@Composable
@Preview
private fun PreviewDoubleSpeedPlayerUpdate() {
  DoubleSpeedPlayerUpdate()
}
