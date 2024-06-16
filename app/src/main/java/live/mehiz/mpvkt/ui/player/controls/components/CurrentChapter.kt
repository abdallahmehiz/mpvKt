package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.ui.theme.spacing

@Composable
fun CurrentChapter(
  chapter: MPVView.Chapter,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(25))
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6F))
      .clickable(onClick = onClick)
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.smaller),
  ) {
    AnimatedContent(
      targetState = chapter,
      transitionSpec = {
        if (targetState.time > initialState.time) {
          (slideInVertically { height -> height } + fadeIn())
            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
        } else {
          (slideInVertically { height -> -height } + fadeIn())
            .togetherWith(slideOutVertically { height -> height } + fadeOut())
        }.using(
          SizeTransform(clip = false),
        )
      },
      label = "Chapter",
    ) { currentChapter ->
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Bookmarks,
          contentDescription = null,
          modifier = Modifier
            .padding(end = 8.dp)
            .size(16.dp),
          tint = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = Utils.prettyTime(currentChapter.time.toInt()),
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Clip,
          color = MaterialTheme.colorScheme.tertiary,
        )
        currentChapter.title?.let {
          Text(
            text = " • ",
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
          )
          Text(
            text = it,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }
    }
  }
}
