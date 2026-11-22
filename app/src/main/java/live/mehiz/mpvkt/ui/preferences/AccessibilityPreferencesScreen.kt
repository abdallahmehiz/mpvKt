package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference

/**
 * Everything the player says out loud.
 *
 * A top-level settings entry rather than something buried in the subtitle screen: someone who
 * needs this cannot find it by guessing which media category it belongs to.
 */
@Serializable
object AccessibilityPreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = App.graph.accessibilityPreferences

    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(stringResource(R.string.pref_accessibility)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.a11y_btn_back),
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          val announceSubtitles by preferences.announceSubtitles.collectAsState()
          SwitchPreference(
            value = announceSubtitles,
            onValueChange = { preferences.announceSubtitles.set(it) },
            title = { Text(stringResource(R.string.pref_a11y_announce_subtitles_title)) },
            summary = { Text(stringResource(R.string.pref_a11y_announce_subtitles_summary)) },
          )

          val announceSecondary by preferences.announceSecondary.collectAsState()
          SwitchPreference(
            value = announceSecondary,
            onValueChange = { preferences.announceSecondary.set(it) },
            title = { Text(stringResource(R.string.pref_a11y_announce_secondary_title)) },
            summary = { Text(stringResource(R.string.pref_a11y_announce_secondary_summary)) },
          )

          val interrupt by preferences.interruptOnNewLine.collectAsState()
          SwitchPreference(
            value = interrupt,
            onValueChange = { preferences.interruptOnNewLine.set(it) },
            title = { Text(stringResource(R.string.pref_a11y_interrupt_title)) },
            summary = { Text(stringResource(R.string.pref_a11y_interrupt_summary)) },
          )

          val onlyWhenVisible by preferences.onlyWhenVisible.collectAsState()
          SwitchPreference(
            value = onlyWhenVisible,
            onValueChange = { preferences.onlyWhenVisible.set(it) },
            title = { Text(stringResource(R.string.pref_a11y_only_when_visible_title)) },
            summary = { Text(stringResource(R.string.pref_a11y_only_when_visible_summary)) },
          )
        }
      }
    }
  }
}
