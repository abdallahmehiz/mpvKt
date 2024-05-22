package live.mehiz.mpvkt.ui.preferences

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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.PlayerOrientation
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.switchPreference
import org.koin.compose.koinInject

object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    val doubleTapToPause by preferences.doubleTapToPause.collectAsState()
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
          item {
            val orientation by preferences.orientation.collectAsState()
            ListPreference(
              value = orientation,
              onValueChange = { preferences.orientation.set(it) },
              values = PlayerOrientation.entries,
              valueToText = { AnnotatedString(context.getString(it.titleRes)) },
              title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
              summary = { Text(text = stringResource(id = orientation.titleRes)) },
            )
          }
          switchPreference(
            key = preferences.doubleTapToPause.key(),
            defaultValue = preferences.doubleTapToPause.defaultValue(),
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_to_pause)) },
          )
          switchPreference(
            key = preferences.doubleTapToSeek.key(),
            defaultValue = preferences.doubleTapToSeek.defaultValue(),
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_to_seek)) },
            enabled = { !doubleTapToPause },
          )
          listPreference(
            key = preferences.doubleTapToSeekDuration.key(),
            defaultValue = 10,
            values = listOf(5, 10, 15, 20, 25, 30),
            valueToText = { AnnotatedString("${it}s") },
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration)) },
            summary = { Text(text = "${it}s") },
            enabled = { !doubleTapToPause && doubleTapToSeek },
          )
        }
      }
    }
  }
}
