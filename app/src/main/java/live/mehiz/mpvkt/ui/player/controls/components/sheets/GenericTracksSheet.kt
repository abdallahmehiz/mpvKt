package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.ui.player.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericTracksSheet(
  tracks: List<T>,
  track: @Composable (T) -> Unit,
  footer: @Composable () -> Unit = {},
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
    footer()
    Spacer(modifier = Modifier.padding(vertical = 32.dp))
  }
}

@Composable
fun getTrackTitle(track: Track): String {
  return when {
    track.id == -1 -> {
      track.name
    }
    track.language.isNullOrBlank() && track.name.isNotBlank() -> {
      stringResource(R.string.player_sheets_track_title_wo_lang, track.id, track.name)
    }
    !track.language.isNullOrBlank() && track.name.isNotBlank() -> {
      stringResource(R.string.player_sheets_track_title_w_lang, track.id, track.name, track.language)
    }
    !track.language.isNullOrBlank() && track.name.isBlank() -> {
      stringResource(R.string.player_sheets_track_lang_wo_title, track.id, track.language)
    }
    else -> stringResource(R.string.player_sheets_track_title_wo_lang, track.id, track.name)
  }
}

enum class Sheets {
  None,
  SubtitlesSheet,
  AudioSheet,
  Chapters,
  ;
}
