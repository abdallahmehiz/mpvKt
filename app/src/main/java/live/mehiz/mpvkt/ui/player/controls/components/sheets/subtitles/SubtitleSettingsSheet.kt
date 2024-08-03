package live.mehiz.mpvkt.ui.player.controls.components.sheets.subtitles

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.presentation.components.SliderItem
import live.mehiz.mpvkt.presentation.components.VerticalSliderItem
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubtitleSettingsSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onDismissRequest)
  val preferences = koinInject<SubtitlesPreferences>()

  val orientation = LocalConfiguration.current.orientation

  ConstraintLayout(modifier = modifier.fillMaxSize()) {
    val vposSlider = createRef()
    val subSettingsCards = createRef()
    val cards: @Composable (Int, Modifier) -> Unit = { value, cardsModifier ->
      when (value) {
        0 -> SubtitleSettingsTypographyCard(cardsModifier)
        1 -> SubtitleSettingsColorsCard(cardsModifier)
        2 -> SubtitlesMiscellaneousCard(cardsModifier)
        else -> {}
      }
    }

    val pagerState = rememberPagerState { 3 }
    if (orientation == ORIENTATION_PORTRAIT) {
      HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp * 0.9f),
        verticalAlignment = Alignment.Top,
        pageSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 8.dp),
        beyondBoundsPageCount = 1,
        modifier = Modifier.constrainAs(subSettingsCards) {
          top.linkTo(parent.top, 32.dp)
          start.linkTo(parent.start)
        },
      ) { page ->
        cards(page, Modifier.fillMaxWidth())
      }
    } else {
      Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.constrainAs(subSettingsCards) {
          top.linkTo(parent.top)
          end.linkTo(parent.end, 32.dp)
        },
      ) {
        Spacer(Modifier.height(16.dp))
        repeat(3) { cards(it, Modifier) }
        Spacer(Modifier.height(16.dp))
      }
    }

    val position by preferences.position.collectAsState()
    if (orientation == ORIENTATION_LANDSCAPE) {
      VerticalSliderItem(
        stringResource(R.string.player_sheets_sub_position),
        position,
        position.toString(),
        onChange = {
          preferences.position.set(it)
          MPVLib.setPropertyInt("sub-pos", it)
        },
        max = 150,
        icon = {
          IconButton(onClick = { MPVLib.setPropertyInt("sub-pos", preferences.position.deleteAndGet()) }) {
            Icon(Icons.Default.Refresh, null)
          }
        },
        modifier = Modifier
          .constrainAs(vposSlider) {
            start.linkTo(parent.start)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          }
          .windowInsetsPadding(WindowInsets.displayCutout),
      )
    } else {
      Card(
        colors = SubtitleSettingsCardColors(),
        modifier = Modifier
          .constrainAs(vposSlider) {
            start.linkTo(parent.start)
            bottom.linkTo(parent.bottom, 16.dp)
            end.linkTo(parent.end)
          }
          .padding(horizontal = 16.dp),
      ) {
        SliderItem(
          label = stringResource(R.string.player_sheets_sub_position),
          position,
          valueText = position.toString(),
          onChange = {
            preferences.position.set(it)
            MPVLib.setPropertyInt("sub-pos", it)
          },
          max = 150,
        )
      }
    }
  }
}

val CARDS_MAX_WIDTH = 420.dp
val SubtitleSettingsCardColors: @Composable () -> CardColors = {
  val colors = CardDefaults.cardColors()
  colors.copy(
    containerColor = colors.containerColor.copy(0.6f),
    disabledContainerColor = colors.disabledContainerColor.copy(0.6f),
  )
}
