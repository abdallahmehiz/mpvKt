package live.mehiz.mpvkt.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.presentation.components.SliderItem
import live.mehiz.mpvkt.ui.player.VideoFilters
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.theme.spacing
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Composable
fun VideoFiltersPanel(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ConstraintLayout(
    modifier = modifier
      .fillMaxSize()
      .padding(MaterialTheme.spacing.medium),
  ) {
    val filtersCard = createRef()

    FiltersCard(
      Modifier.constrainAs(filtersCard) {
        linkTo(parent.top, parent.bottom, bias = 0.8f)
        end.linkTo(parent.end)
      },
      onClose = onDismissRequest,
    )
  }
}

@Composable
fun FiltersCard(
  modifier: Modifier = Modifier,
  onClose: () -> Unit,
) {
  val decoderPreferences = koinInject<DecoderPreferences>()
  Card(
    colors = panelCardsColors(),
    modifier = modifier
      .widthIn(max = CARDS_MAX_WIDTH),
  ) {
    Row(
      Modifier
        .fillMaxWidth()
        .padding(start = MaterialTheme.spacing.medium),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        stringResource(id = R.string.player_sheets_filters_title),
        style = MaterialTheme.typography.headlineMedium,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
      ) {
        TextButton(
          onClick = {
            VideoFilters.entries.forEach {
              MPVLib.setPropertyInt(it.mpvProperty, it.preference(decoderPreferences).deleteAndGet())
            }
          },
        ) {
          Text(text = stringResource(id = R.string.generic_reset))
        }
        ControlsButton(Icons.Default.Close, onClose)
      }
    }
    LazyColumn {
      items(VideoFilters.entries) { filter ->
        val value by filter.preference(decoderPreferences).collectAsState()
        SliderItem(
          label = stringResource(filter.titleRes),
          value = value,
          valueText = value.toString(),
          onChange = {
            filter.preference(decoderPreferences).set(it)
            MPVLib.setPropertyInt(filter.mpvProperty, it)
          },
          max = 100,
          min = -100,
        )
      }
      item {
        if (decoderPreferences.gpuNext.get()) return@item
        ProvidePreferenceLocals {
          FooterPreference(
            summary = {
              Text(text = stringResource(id = R.string.player_sheets_filters_warning))
            },
          )
        }
      }
    }
  }
}
