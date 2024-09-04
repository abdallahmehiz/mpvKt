package live.mehiz.mpvkt.ui.player.controls

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DecodersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.MoreSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.PlaybackSpeedSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet
import org.koin.compose.koinInject

@Composable
fun PlayerSheets() {
  val viewModel = koinInject<PlayerViewModel>()
  val subtitles by viewModel.subtitleTracks.collectAsState()
  val selectedSubs by viewModel.selectedSubtitles.collectAsState()
  val audioTracks by viewModel.audioTracks.collectAsState()
  val selectedAudio by viewModel.selectedAudio.collectAsState()
  val sheetShown by viewModel.sheetShown.collectAsState()
  val onDismissRequest: () -> Unit = {
    viewModel.sheetShown.update { Sheets.None }
    viewModel.showControls()
  }

  when (sheetShown) {
    Sheets.None -> {}
    Sheets.SubtitleTracks -> {
      val subtitlesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        viewModel.addSubtitle(it)
      }
      SubtitlesSheet(
        tracks = subtitles.toImmutableList(),
        selectedTracks = selectedSubs.toList().toImmutableList(),
        onSelect = { viewModel.selectSub(it) },
        onAddSubtitle = { subtitlesPicker.launch(arrayOf("*/*")) },
        onOpenSubtitleSettings = {
          viewModel.panelShown.update { Panels.SubtitleSettings }
          onDismissRequest()
        },
        onOpenSubtitleDelay = {
          viewModel.panelShown.update { Panels.SubtitleDelay }
          onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.AudioTracks -> {
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
        onOpenDelayPanel = {
          viewModel.panelShown.update { Panels.AudioDelay }
          onDismissRequest()
        },
        onDismissRequest,
      )
    }

    Sheets.Chapters -> {
      val chapter by viewModel.currentChapter.collectAsState()
      ChaptersSheet(
        viewModel.chapters.toImmutableList(),
        currentChapter = chapter ?: viewModel.chapters[0],
        onClick = {
          viewModel.selectChapter(viewModel.chapters.indexOf(it))
          onDismissRequest()
          viewModel.unpause()
        },
        onDismissRequest,
      )
    }

    Sheets.Decoders -> {
      val currentDecoder by viewModel.currentDecoder.collectAsState()
      DecodersSheet(
        selectedDecoder = currentDecoder,
        onSelect = { viewModel.updateDecoder(it) },
        onDismissRequest,
      )
    }

    Sheets.More -> {
      MoreSheet(
        onDismissRequest,
        onEnterFiltersPanel = {
          viewModel.panelShown.update { Panels.VideoFilters }
          onDismissRequest()
        }
      )
    }

    Sheets.PlaybackSpeed -> {
      PlaybackSpeedSheet(onDismissRequest = onDismissRequest)
    }
  }
}
