package live.mehiz.mpvkt.ui.preferences

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerOrientation
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import org.koin.compose.koinInject

object PlayerPreferencesScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    val doubleTapToSeek by preferences.doubleTapToSeek.collectAsState()
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
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          listPreference(
            preferences.orientation.key(),
            defaultValue = preferences.orientation.defaultValue().name,
            values = PlayerOrientation.entries.map { it.name }.toImmutableList(),
            valueToText = { AnnotatedString(context.getString(enumValueOf<PlayerOrientation>(it).titleRes)) },
            title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
            summary = { Text(text = stringResource(id = enumValueOf<PlayerOrientation>(it).titleRes)) },
          )
          switchPreference(
            key = preferences.drawOverDisplayCutout.key(),
            defaultValue = preferences.drawOverDisplayCutout.defaultValue(),
            title = { Text(stringResource(R.string.pref_player_draw_over_cutout)) },
          )
          switchPreference(
            key = preferences.savePositionOnQuit.key(),
            defaultValue = preferences.savePositionOnQuit.defaultValue(),
            title = { Text(stringResource(R.string.pref_player_save_position_on_quit)) },
          )
          switchPreference(
            key = preferences.doubleTapToPause.key(),
            defaultValue = preferences.doubleTapToPause.defaultValue(),
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_to_pause)) },
          )
          switchPreference(
            key = preferences.doubleTapToSeek.key(),
            defaultValue = preferences.doubleTapToSeek.defaultValue(),
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_to_seek)) },
          )
          listPreference(
            key = preferences.doubleTapToSeekDuration.key(),
            defaultValue = preferences.doubleTapToSeekDuration.defaultValue(),
            values = listOf(3, 5, 10, 15, 20, 25, 30),
            valueToText = { AnnotatedString("${it}s") },
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration)) },
            summary = { Text(text = "${it}s") },
            enabled = { doubleTapToSeek },
          )
          if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            switchPreference(
              key = preferences.automaticallyEnterPip.key(),
              defaultValue = preferences.automaticallyEnterPip.defaultValue(),
              title = { Text(text = stringResource(id = R.string.pref_player_automatically_enter_pip)) },
            )
          }
          switchPreference(
            key = preferences.closeAfterReachingEndOfVideo.key(),
            defaultValue = preferences.closeAfterReachingEndOfVideo.defaultValue(),
            title = { Text(text = stringResource(id = R.string.pref_player_close_after_eof)) }
          )
          preferenceCategory(
            "gestures",
            title = { Text(stringResource(R.string.pref_player_gestures)) },
          )
          switchPreference(
            preferences.horizontalSeekGesture.key(),
            defaultValue = preferences.horizontalSeekGesture.get(),
            title = { Text(stringResource(R.string.pref_player_gestures_seek)) },
          )
          switchPreference(
            preferences.brightnessGesture.key(),
            defaultValue = preferences.brightnessGesture.get(),
            title = { Text(stringResource(R.string.pref_player_gestures_brightness)) },
          )
          switchPreference(
            preferences.volumeGesture.key(),
            defaultValue = preferences.volumeGesture.get(),
            title = { Text(stringResource(R.string.pref_player_gestures_volume)) },
          )
          switchPreference(
            preferences.holdForDoubleSpeed.key(),
            defaultValue = preferences.holdForDoubleSpeed.defaultValue(),
            title = { Text(stringResource(R.string.pref_player_gestures_hold_for_double_speed)) },
          )
          preferenceCategory(
            "controls",
            title = { Text(stringResource(R.string.pref_player_controls)) },
          )
          switchPreference(
            preferences.allowGesturesInPanels.key(),
            defaultValue = preferences.allowGesturesInPanels.defaultValue(),
            title = { Text(text = "Allow gestures in panels") },
          )
          switchPreference(
            preferences.showChaptersButton.key(),
            defaultValue = preferences.showChaptersButton.defaultValue(),
            title = { Text(stringResource(R.string.pref_player_controls_show_chapters_button)) },
            summary = { Text(stringResource(R.string.pref_player_controls_show_chapters_summary)) },
          )
          switchPreference(
            preferences.currentChaptersIndicator.key(),
            defaultValue = preferences.currentChaptersIndicator.defaultValue(),
            title = { Text(stringResource(R.string.pref_player_controls_show_chapter_indicator)) },
            summary = { Text(stringResource(R.string.pref_player_controls_show_chapters_summary)) },
          )
        }
      }
    }
  }
}
