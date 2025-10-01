package live.mehiz.mpvkt.ui.preferences

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import me.zhanghai.compose.preference.TwoTargetIconButtonPreference
import org.koin.compose.koinInject

@Serializable
object SubtitlesPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val preferences = koinInject<SubtitlesPreferences>()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_subtitles))
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
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
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          val preferredLanguages by preferences.preferredLanguages.collectAsState()
          TextFieldPreference(
            value = preferredLanguages,
            onValueChange = preferences.preferredLanguages::set,
            textToValue = { it },
            title = { Text(stringResource(R.string.pref_preferred_languages)) },
            summary = { if (preferredLanguages.isNotBlank()) Text(preferredLanguages) },
            textField = { value, onValueChange, _ ->
              Column {
                Text(stringResource(R.string.pref_subtitles_preferred_language))
                TextField(
                  value,
                  onValueChange,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            },
          )
          TwoTargetIconButtonPreference(
            title = { Text(stringResource(R.string.pref_subtitles_fonts_dir)) },
            onClick = { locationPicker.launch(null) },
            summary = {
              if (fontsFolder.isBlank()) return@TwoTargetIconButtonPreference
              Text(getSimplifiedPathFromUri(fontsFolder))
            },
            iconButtonIcon = { Icon(Icons.Default.Clear, null) },
            onIconButtonClick = { preferences.fontsFolder.delete() },
            iconButtonEnabled = fontsFolder.isNotBlank()
          )
          val autoloadExternal by preferences.autoLoadExternal.collectAsState()
          SwitchPreference(
            value = autoloadExternal,
            onValueChange = { preferences.autoLoadExternal.set(it) },
            title = { Text(text = stringResource(id = R.string.pref_subtitles_autoload_title)) },
            summary = {
              Text(
                text = stringResource(id = R.string.pref_subtitles_autoload_summary),
              )
            },
          )
        }
      }
    }
  }
}
