package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import live.mehiz.mpvkt.ui.player.Decoder

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  GenericTracksSheet(
    Decoder.entries.minusElement(Decoder.Auto),
    track = {
      AudioTrackRow(
        title = it.title,
        isSelected = selectedDecoder == it,
      ) {
        onSelect(it)
      }
    },
    onDismissRequest = onDismissRequest
  )
}
