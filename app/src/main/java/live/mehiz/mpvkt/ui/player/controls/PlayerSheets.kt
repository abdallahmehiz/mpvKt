package live.mehiz.mpvkt.ui.player.controls

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DecodersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.MoreSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet

@Composable
fun PlayerSheets(viewModel: PlayerViewModel) {
  val subtitles by viewModel.subtitleTracks.collectAsState()
  val selectedSubs by viewModel.selectedSubtitles.collectAsState()
  val audioTracks by viewModel.audioTracks.collectAsState()
  val selectedAudio by viewModel.selectedAudio.collectAsState()
  val sheetShown by viewModel.sheetShown.collectAsState()
  val onDismissRequest: () -> Unit = {
    viewModel.sheetShown.update { Sheets.None }
  }
  when (sheetShown) {
    Sheets.None -> {}
    Sheets.SubtitlesSheet -> {
      val subtitlesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        viewModel.addSubtitle(it)
      }
      SubtitlesSheet(
        subtitles.toImmutableList(),
        selectedSubs.toImmutableList(),
        { viewModel.selectSub(it) },
        { subtitlesPicker.launch(arrayOf("*/*")) },
        onDismissRequest
      )
    }

    Sheets.AudioSheet -> {
      val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        viewModel.addAudio(it)
      }
      AudioTracksSheet(
        audioTracks.toImmutableList(),
        selectedAudio,
        { viewModel.selectAudio(it) },
        { audioPicker.launch(arrayOf("*/*")) },
        onDismissRequest
      )
    }

    Sheets.Chapters -> {
      ChaptersSheet(
        viewModel.chapters.toImmutableList(),
        currentChapter = viewModel.currentChapter.value?.index ?: 0,
        onClick = {
          viewModel.selectChapter(it)
          onDismissRequest()
          viewModel.unpause()
        },
        onDismissRequest
      )
    }

    Sheets.Decoders -> {
      val currentDecoder by viewModel.currentDecoder.collectAsState()
      DecodersSheet(
        selectedDecoder = currentDecoder,
        onSelect = { viewModel.updateDecoder(it) },
        onDismissRequest
      )
    }

    Sheets.More -> {
      MoreSheet(onDismissRequest)
    }
  }
}
