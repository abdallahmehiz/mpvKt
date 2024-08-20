package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.components.panels.AudioDelayPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitleDelayPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitleSettingsPanel
import live.mehiz.mpvkt.ui.player.controls.components.panels.VideoFiltersPanel
import org.koin.compose.koinInject

@Composable
fun PlayerPanels(modifier: Modifier = Modifier) {
  val viewModel = koinInject<PlayerViewModel>()
  val panelShown by viewModel.panelShown.collectAsState()
  val onDismissRequest: () -> Unit = {
    viewModel.panelShown.update { Panels.None }
    viewModel.showControls()
  }

  AnimatedContent(
    targetState = panelShown,
    label = "panels",
    contentAlignment = Alignment.CenterEnd,
    contentKey = { it.name },
    transitionSpec = {
      fadeIn() + slideInHorizontally { it / 3 } togetherWith fadeOut() + slideOutHorizontally { it / 2 }
    },
    modifier = modifier
  ) { currentPanel ->
    when (currentPanel) {
      Panels.None -> { Box(Modifier.fillMaxHeight()) }
      Panels.SubtitleSettings -> {
        viewModel.hideControls()
        SubtitleSettingsPanel(onDismissRequest)
      }
      Panels.SubtitleDelay -> {
        viewModel.hideControls()
        SubtitleDelayPanel(onDismissRequest)
      }
      Panels.AudioDelay -> {
        viewModel.hideControls()
        AudioDelayPanel(onDismissRequest)
      }

      Panels.VideoFilters -> {
        viewModel.hideControls()
        VideoFiltersPanel(onDismissRequest)
      }
    }
  }
}
