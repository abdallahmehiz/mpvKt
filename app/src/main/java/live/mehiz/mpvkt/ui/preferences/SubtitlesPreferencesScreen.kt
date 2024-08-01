package live.mehiz.mpvkt.ui.preferences

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.textFieldPreference
import org.koin.compose.koinInject

object SubtitlesPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val preferences = koinInject<SubtitlesPreferences>()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_preferred_languages))
          },
          navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        val locationPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
          if (uri == null) return@rememberLauncherForActivityResult

          val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
          context.contentResolver.takePersistableUriPermission(uri, flags)
          preferences.fontsFolder.set(uri.toString())
        }
        val fontsFolder by preferences.fontsFolder.collectAsState()
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          textFieldPreference(
            preferences.preferredLanguages.key(),
            defaultValue = preferences.preferredLanguages.defaultValue(),
            textToValue = { it },
            title = { Text(stringResource(R.string.pref_preferred_languages)) },
            summary = { if (it.isNotBlank()) Text(it) },
            textField = { value, onValueChange, _ ->
              Column {
                Text(stringResource(`is`.xyz.mpv.R.string.pref_default_subtitle_language_message))
                TextField(
                  value,
                  onValueChange,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            },
          )
          preference(
            preferences.fontsFolder.key(),
            title = { Text(stringResource(R.string.pref_subtitles_fonts_dir)) },
            onClick = { locationPicker.launch(null) },
            summary = {
              if (fontsFolder.isBlank()) return@preference
              Text(getSimplifiedPathFromUri(fontsFolder))
            }
          )
        }
      }
    }
  }
}
