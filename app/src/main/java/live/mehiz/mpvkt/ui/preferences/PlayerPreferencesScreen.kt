package live.mehiz.mpvkt.ui.preferences

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerOrientation
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

object PlayerPreferencesScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.pref_player)) },
          navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          val orientation by preferences.orientation.collectAsState()
          ListPreference(
            value = orientation,
            onValueChange = preferences.orientation::set,
            values = PlayerOrientation.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
            summary = { Text(text = stringResource(id = orientation.titleRes)) },
          )
          val drawOverDisplayCutout by preferences.drawOverDisplayCutout.collectAsState()
          SwitchPreference(
            value = drawOverDisplayCutout,
            onValueChange = preferences.drawOverDisplayCutout::set,
            title = { Text(stringResource(R.string.pref_player_draw_over_cutout)) },
          )
          val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
          SwitchPreference(
            value = savePositionOnQuit,
            onValueChange = preferences.savePositionOnQuit::set,
            title = { Text(stringResource(R.string.pref_player_save_position_on_quit)) },
          )
          if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val enterPiPAutomatically by preferences.automaticallyEnterPip.collectAsState()
            SwitchPreference(
              value = enterPiPAutomatically,
              onValueChange = preferences.automaticallyEnterPip::set,
              title = { Text(text = stringResource(id = R.string.pref_player_automatically_enter_pip)) },
            )
          }
          val closeAtEOF by preferences.closeAfterReachingEndOfVideo.collectAsState()
          SwitchPreference(
            value = closeAtEOF,
            onValueChange = preferences.closeAfterReachingEndOfVideo::set,
            title = { Text(text = stringResource(id = R.string.pref_player_close_after_eof)) }
          )
          val rememberBrightness by preferences.rememberBrightness.collectAsState()
          SwitchPreference(
            value = rememberBrightness,
            onValueChange = preferences.rememberBrightness::set,
            title = { Text(text = stringResource(R.string.pref_player_remember_brightness)) }
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_seeking_title)) }
          )
          val horizontalSeekGesture by preferences.horizontalSeekGesture.collectAsState()
          SwitchPreference(
            value = horizontalSeekGesture,
            onValueChange = preferences.horizontalSeekGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_seek)) },
          )
          val showSeekbarWhenSeeking by preferences.showSeekBarWhenSeeking.collectAsState()
          SwitchPreference(
            value = showSeekbarWhenSeeking,
            onValueChange = preferences.showSeekBarWhenSeeking::set,
            title = { Text(stringResource(R.string.pref_player_show_seekbar_when_seeking)) }
          )
          val preciseSeeking by preferences.preciseSeeking.collectAsState()
          SwitchPreference(
            value = preciseSeeking,
            onValueChange = preferences.preciseSeeking::set,
            title = { Text(stringResource(R.string.pref_player_precise_seeking_title)) },
            summary = { Text(stringResource(R.string.pref_player_precise_seeking_summary)) }
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_gestures)) },
          )
          val brightnessGesture by preferences.brightnessGesture.collectAsState()
          SwitchPreference(
            value = brightnessGesture,
            onValueChange = preferences.brightnessGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_brightness)) },
          )
          val volumeGesture by preferences.volumeGesture.collectAsState()
          SwitchPreference(
            value = volumeGesture,
            onValueChange = preferences.volumeGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_volume)) },
          )
          val holdForDoubleSpeed by preferences.holdForDoubleSpeed.collectAsState()
          SwitchPreference(
            value = holdForDoubleSpeed,
            onValueChange = preferences.holdForDoubleSpeed::set,
            title = { Text(stringResource(R.string.pref_player_gestures_hold_for_double_speed)) },
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_controls)) },
          )
          val allowGesturesInPanels by preferences.allowGesturesInPanels.collectAsState()
          SwitchPreference(
            value = allowGesturesInPanels,
            onValueChange = preferences.allowGesturesInPanels::set,
            title = { Text(text = stringResource(id = R.string.pref_player_controls_allow_gestures_in_panels)) },
          )
          val displayVolumeAsPercentage by preferences.displayVolumeAsPercentage.collectAsState()
          SwitchPreference(
            value = displayVolumeAsPercentage,
            onValueChange = preferences.displayVolumeAsPercentage::set,
            title = { Text(stringResource(R.string.pref_player_controls_display_volume_as_percentage)) },
          )
          val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
          SwitchPreference(
            value = swapVolumeAndBrightness,
            onValueChange = preferences.swapVolumeAndBrightness::set,
            title = { Text(stringResource(R.string.swap_the_volume_and_brightness_slider)) },
          )
          val showLoadingCircle by preferences.showLoadingCircle.collectAsState()
          SwitchPreference(
            value = showLoadingCircle,
            onValueChange = preferences.showLoadingCircle::set,
            title = { Text(stringResource(R.string.pref_player_controls_show_loading_circle)) }
          )
          val showChaptersButton by preferences.showChaptersButton.collectAsState()
          SwitchPreference(
            value = showChaptersButton,
            onValueChange = preferences.showChaptersButton::set,
            title = { Text(stringResource(R.string.pref_player_controls_show_chapters_button)) },
            summary = { Text(stringResource(R.string.pref_player_controls_show_chapters_summary)) },
          )
          val showChapterIndicator by preferences.currentChaptersIndicator.collectAsState()
          SwitchPreference(
            value = showChapterIndicator,
            onValueChange = preferences.currentChaptersIndicator::set,
            title = { Text(stringResource(R.string.pref_player_controls_show_chapter_indicator)) },
            summary = { Text(stringResource(R.string.pref_player_controls_show_chapters_summary)) },
          )

          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_display)) },
          )

          val showSystemStatusBar by preferences.showSystemStatusBar.collectAsState()
          SwitchPreference(
            value = showSystemStatusBar,
            onValueChange = preferences.showSystemStatusBar::set,
            title = { Text(stringResource(R.string.pref_player_display_show_status_bar)) },
          )
          val reduceMotion by preferences.reduceMotion.collectAsState()
          SwitchPreference(
            value = reduceMotion,
            onValueChange = preferences.reduceMotion::set,
            title = { Text(stringResource(R.string.pref_player_display_reduce_player_animation)) },
          )
          val playerTimeToDisappear by preferences.playerTimeToDisappear.collectAsState()
          ListPreference(
            value = playerTimeToDisappear,
            onValueChange = preferences.playerTimeToDisappear::set,
            values = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000),
            valueToText = { AnnotatedString("$it ms") },
            title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
            summary = { Text(text = "$playerTimeToDisappear ms") },
          )
          val panelTransparency by preferences.panelTransparency.collectAsState()
          SliderPreference(
            value = panelTransparency,
            onValueChange = { preferences.panelTransparency.set(it) },
            title = { Text(stringResource(R.string.pref_player_display_panel_opacity)) },
            valueRange = 0f..1f,
            summary = {
              Text(
                text = stringResource(
                  id = R.string.pref_player_display_panel_transparency_summary,
                  panelTransparency.times(100).toInt(),
                ),
              )
            },
            onSliderValueChange = { preferences.panelTransparency.set(it) },
            sliderValue = panelTransparency,
          )
        }
      }
    }
  }
}
