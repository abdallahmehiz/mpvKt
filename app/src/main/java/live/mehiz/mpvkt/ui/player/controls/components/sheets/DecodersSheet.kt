package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.R
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
        title = stringResource(R.string.player_sheets_decoder_formatted, it.title, it.value),
        isSelected = selectedDecoder == it,
        onClick = { onSelect(it) }
      )
    },
    onDismissRequest = onDismissRequest
  )
}
