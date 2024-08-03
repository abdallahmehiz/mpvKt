package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.presentation.Screen
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference
import org.koin.compose.koinInject

object AudioPreferencesScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val preferences = koinInject<AudioPreferences>()
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_audio))
          },
          navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, null)
            }
          }
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
          textFieldPreference(
            preferences.preferredLanguages.key(),
            defaultValue = preferences.preferredLanguages.defaultValue(),
            textToValue = { it },
            title = { Text(stringResource(R.string.pref_preferred_languages)) },
            summary = { if (it.isNotBlank()) Text(it) },
            textField = { value, onValueChange, _ ->
              Column {
                Text(stringResource(`is`.xyz.mpv.R.string.pref_default_audio_language_message))
                TextField(
                  value,
                  onValueChange,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            },
          )
          switchPreference(
            key = preferences.audioPitchCorrection.key(),
            defaultValue = preferences.audioPitchCorrection.defaultValue(),
            title = { Text(stringResource(R.string.pref_audio_pitch_correction_title)) },
            summary = { Text(stringResource(R.string.pref_audio_pitch_correction_summary)) },
          )
        }
      }
    }
  }
}
