package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericTracksSheet(
  tracks: List<T>,
  track: @Composable (T) -> Unit,
  onDismissRequest: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest,
    modifier = Modifier.padding(32.dp),
  ) {
    LazyColumn {
      items(tracks) {
        track(it)
      }
    }
    Spacer(modifier = Modifier.padding(vertical = 32.dp))
  }
}

enum class Sheets {
  None,
  SubtitlesSheet,
  AudioSheet,
  ;
}
