package live.mehiz.mpvkt.ui.player.controls

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.Track
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DecodersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.MoreSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.PlaybackSpeedSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet

@Composable
fun PlayerSheets(
  sheetShown: Sheets,

  // subtitles sheet
  subtitles: ImmutableList<Track>,
  selectedSubtitles: ImmutableList<Int>,
  onAddSubtitle: (Uri) -> Unit,
  onSelectSubtitle: (Int) -> Unit,
  // audio sheet
  audioTracks: ImmutableList<Track>,
  selectedAudio: Int,
  onAddAudio: (Uri) -> Unit,
  onSelectAudio: (Int) -> Unit,
  // chapters sheet
  chapter: Segment?,
  chapters: ImmutableList<Segment>,
  onSeekToChapter: (Int) -> Unit,
  // Decoders sheet
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  // Speed sheet
  speed: Float,
  onSpeedChange: (Float) -> Unit,
  // More sheet
  sleepTimerTimeRemaining: Int,
  onStartSleepTimer: (Int) -> Unit,
  buttons: ImmutableList<CustomButtonEntity>,

  onOpenPanel: (Panels) -> Unit,
  onDismissRequest: () -> Unit,
) {
  when (sheetShown) {
    Sheets.None -> {}
    Sheets.SubtitleTracks -> {
      val subtitlesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        onAddSubtitle(it)
      }
      SubtitlesSheet(
        tracks = subtitles.toImmutableList(),
        selectedTracks = selectedSubtitles,
        onSelect = onSelectSubtitle,
        onAddSubtitle = { subtitlesPicker.launch(arrayOf("*/*")) },
        onOpenSubtitleSettings = { onOpenPanel(Panels.SubtitleSettings) },
        onOpenSubtitleDelay = { onOpenPanel(Panels.SubtitleDelay) },
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.AudioTracks -> {
      val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        onAddAudio(it)
      }
      AudioTracksSheet(
        tracks = audioTracks,
        selectedId = selectedAudio,
        onSelect = onSelectAudio,
        onAddAudioTrack = { audioPicker.launch(arrayOf("*/*")) },
        onOpenDelayPanel = { onOpenPanel(Panels.AudioDelay) },
        onDismissRequest,
      )
    }

    Sheets.Chapters -> {
      if (chapter == null) return
      ChaptersSheet(
        chapters,
        currentChapter = chapter,
        onClick = { onSeekToChapter(chapters.indexOf(chapter)) },
        onDismissRequest,
      )
    }

    Sheets.Decoders -> {
      DecodersSheet(
        selectedDecoder = decoder,
        onSelect = onUpdateDecoder,
        onDismissRequest,
      )
    }

    Sheets.More -> {
      MoreSheet(
        remainingTime = sleepTimerTimeRemaining,
        onStartTimer = onStartSleepTimer,
        onDismissRequest = onDismissRequest,
        onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
        customButtons = buttons,
      )
    }

    Sheets.PlaybackSpeed -> {
      PlaybackSpeedSheet(
        speed,
        onSpeedChange = onSpeedChange,
        onDismissRequest = onDismissRequest
      )
    }
  }
}
