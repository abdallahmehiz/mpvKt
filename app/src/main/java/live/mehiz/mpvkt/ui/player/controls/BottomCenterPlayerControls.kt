package live.mehiz.mpvkt.ui.player.controls

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.ui.player.execute
import live.mehiz.mpvkt.ui.player.executeLongClick
import androidx.compose.foundation.combinedClickable
import live.mehiz.mpvkt.ui.theme.spacing
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun BottomCenterPlayerControls(
    customButtons: List<CustomButtonEntity>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .fillMaxWidth()
    ) {
        customButtons.forEach { customButton ->
            Box(
                modifier = Modifier
                    .padding(end = MaterialTheme.spacing.smaller)
                    .widthIn(min = 5.dp)
            ) {
                Button(onClick = {},
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Text(
                        text = customButton.title,
                        color = Color.White,
                        maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            onClick = customButton::execute,
                            onLongClick = customButton::executeLongClick,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                )
            }
        }
    }
}
