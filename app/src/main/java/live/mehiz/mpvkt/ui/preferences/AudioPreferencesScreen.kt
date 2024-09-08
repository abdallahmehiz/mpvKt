package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioChannels
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.koinInject

object AudioPreferencesScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
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
          val preferredLanguages by preferences.preferredLanguages.collectAsState()
          TextFieldPreference(
            value = preferredLanguages,
            onValueChange = { preferences.preferredLanguages.set(it) },
            textToValue = { it },
            title = { Text(stringResource(R.string.pref_preferred_languages)) },
            summary = { if (preferredLanguages.isNotBlank()) Text(preferredLanguages) },
            textField = { value, onValueChange, _ ->
              Column {
                Text(stringResource(R.string.pref_audio_preferred_language))
                TextField(
                  value,
                  onValueChange,
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            },
          )
          val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
          SwitchPreference(
            value = audioPitchCorrection,
            onValueChange = { preferences.audioPitchCorrection.set(it) },
            title = { Text(stringResource(R.string.pref_audio_pitch_correction_title)) },
            summary = { Text(stringResource(R.string.pref_audio_pitch_correction_summary)) },
          )
          val audioChannel by preferences.audioChannels.collectAsState()
          ListPreference(
            value = audioChannel,
            onValueChange = { preferences.audioChannels.set(it) },
            values = AudioChannels.entries,
            valueToText = { AnnotatedString(context.getString(it.title)) },
            title = { Text(text = stringResource(id = R.string.pref_audio_channels)) },
            summary = { Text(text = context.getString(audioChannel.title)) },
          )
          val volumeBoostCap by preferences.volumeBoostCap.collectAsState()
          SliderPreference(
            value = volumeBoostCap.toFloat(),
            onValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
            title = { Text(stringResource(R.string.pref_audio_volume_boost_cap)) },
            valueRange = 0f..200f,
            summary = {
              Text(
                if (volumeBoostCap == 0) {
                  stringResource(R.string.generic_disabled)
                } else {
                  volumeBoostCap.toString()
                },
              )
            },
            onSliderValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
            sliderValue = volumeBoostCap.toFloat(),
          )
        }
      }
    }
  }
}
