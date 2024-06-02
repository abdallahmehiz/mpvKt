package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.ui.player.Track

@Composable
fun AudioTracksSheet(
  tracks: List<Track>,
  selectedId: Int,
  onSelect: (Int) -> Unit,
  onAddAudioTrack: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  GenericTracksSheet(
    tracks,
    onDismissRequest = onDismissRequest,
    header = { AddTrackRow(stringResource(R.string.player_sheets_add_ext_audio), onAddAudioTrack) },
    track = {
      AudioTrackRow(
        title = getTrackTitle(it),
        isSelected = selectedId == it.id,
      ) { onSelect(it.id) }
    },
  )
}

@Composable
fun AudioTrackRow(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(start = 8.dp, end = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    RadioButton(
      isSelected,
      onClick,
    )
    Text(
      title,
      fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
      fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
    )
  }
}
