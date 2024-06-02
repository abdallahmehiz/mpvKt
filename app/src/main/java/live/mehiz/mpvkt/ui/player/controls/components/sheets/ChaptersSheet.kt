package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.R

@Composable
fun ChaptersSheet(
  chapters: List<MPVView.Chapter>,
  currentChapter: Int,
  onClick: (Int) -> Unit,
  onDismissRequest: () -> Unit,
) {
  GenericTracksSheet(
    chapters,
    track = {
      ChapterTrack(
        title = it.title,
        time = it.time.toInt(),
        index = it.index,
        selected = currentChapter == it.index
      ) { onClick(it.index) }
    },
    onDismissRequest = onDismissRequest,
  )
}

@Composable
fun ChapterTrack(
  title: String?,
  time: Int,
  index: Int,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(R.string.player_sheets_track_title_wo_lang, index + 1, title ?: ""),
      fontStyle = if(selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if(selected) FontWeight.ExtraBold else FontWeight.Normal,
      maxLines = 1,
      modifier = Modifier.weight(1f),
      overflow = TextOverflow.Ellipsis
    )
    Text(
      Utils.prettyTime(time),
      fontStyle = if(selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if(selected) FontWeight.ExtraBold else FontWeight.Normal,
    )
  }
}
