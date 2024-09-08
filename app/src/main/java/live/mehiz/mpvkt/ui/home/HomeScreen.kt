package live.mehiz.mpvkt.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.k1rakishou.fsaf.FileManager
import `is`.xyz.mpv.Utils.PROTOCOLS
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.preferences.PreferencesScreen
import live.mehiz.mpvkt.ui.theme.spacing

object HomeScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.app_name)) },
          actions = {
            IconButton(onClick = { navigator.push(PreferencesScreen) }) {
              Icon(Icons.Default.Settings, null)
            }
          },
          navigationIcon = {
            Image(
              painter = painterResource(id = R.drawable.ic_launcher_foreground),
              contentDescription = "app_logo",
            )
          },
        )
      },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        var uri by remember { mutableStateOf("") }
        var isUrlValid by remember { mutableStateOf(true) }
        OutlinedTextField(
          value = uri,
          label = { Text(stringResource(R.string.home_url_input_label)) },
          onValueChange = {
            uri = it
            isUrlValid = it.isBlank() || isURLValid(it)
          },
          supportingText = {
            Text(if (isUrlValid) "" else stringResource(R.string.home_invalid_protocol))
          },
          trailingIcon = {
            if (!isUrlValid) Icon(Icons.Filled.Info, null)
          },
          isError = !isUrlValid,
          maxLines = 5,
        )
        Button(
          onClick = { playFile(uri, context) },
          enabled = uri.isNotBlank() && isUrlValid,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.Link, null)
            Text(text = stringResource(R.string.home_open_url))
          }
        }
        val documentPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocument(),
        ) {
          if (it == null) return@rememberLauncherForActivityResult
          playFile(it.toString(), context)
        }
        OutlinedButton(
          onClick = { documentPicker.launch(arrayOf("*/*")) },
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.FileOpen, null)
            Text(text = stringResource(R.string.home_pick_file))
          }
        }
        val fileManager = FileManager(context)
        val directoryPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocumentTree(),
        ) {
          if (it == null) return@rememberLauncherForActivityResult
          navigator.push(FilePickerScreen(fileManager.fromUri(it)!!.getFullPath()))
        }
        OutlinedButton(onClick = { directoryPicker.launch(null) }) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.FolderOpen, null)
            Text(text = stringResource(R.string.home_open_file_picker))
          }
        }
      }
    }
  }

  // Basically a copy of:
  // https://github.com/mpv-android/mpv-android/blob/32cbff3cedea73b4616b34542cb95bf1d00504cc/app/src/main/java/is/xyz/mpv/Utils.kt#L406
  private fun isURLValid(url: String): Boolean {
    val uri = Uri.parse(url)
    return uri.isHierarchical && !uri.isRelative &&
      !(uri.host.isNullOrBlank() && uri.path.isNullOrBlank()) &&
      PROTOCOLS.contains(uri.scheme)
  }

  fun playFile(
    filepath: String,
    context: Context,
  ) {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
    i.setClass(context, PlayerActivity::class.java)
    context.startActivity(i)
  }
}
