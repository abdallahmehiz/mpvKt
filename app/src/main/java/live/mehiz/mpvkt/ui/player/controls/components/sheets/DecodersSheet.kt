package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.ui.player.Decoder

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  GenericTracksSheet(
    Decoder.entries.minusElement(Decoder.Auto).toImmutableList(),
    track = {
      AudioTrackRow(
        title = it.title,
        isSelected = selectedDecoder == it,
        onClick = { onSelect(it) }
      )
    },
    onDismissRequest = onDismissRequest
  )
}
