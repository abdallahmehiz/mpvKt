package live.mehiz.mpvkt.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastJoinToString
import androidx.documentfile.provider.DocumentFile
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.TextFieldPreference
import me.zhanghai.compose.preference.preference
import org.koin.compose.koinInject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines

object AdvancedPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val preferences = koinInject<AdvancedPreferences>()

    val getMPVConfLocation: (String) -> String = {
      Environment.getExternalStorageDirectory().canonicalPath + "/" + Uri.decode(it).substringAfterLast(":")
    }
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_advanced))
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
        val locationPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
          if (uri == null) return@rememberLauncherForActivityResult

          val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
          context.contentResolver.takePersistableUriPermission(uri, flags)
          preferences.mpvConfStorageUri.set(uri.toString())
        }
        val mpvConfStorageLocation by preferences.mpvConfStorageUri.collectAsState()
        LazyColumn(
          Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          preference(
            "mpv_storage_location",
            title = { Text(stringResource(R.string.pref_advanced_mpv_conf_storage_location)) },
            summary = {
              if (mpvConfStorageLocation.isNotBlank()) {
                Text(getMPVConfLocation(mpvConfStorageLocation))
              }
            },
            onClick = {
              locationPicker.launch(null)
            },
          )
          item {
            var mpvConf by remember { mutableStateOf(preferences.mpvConf.get()) }
            LaunchedEffect(true) {
              if (mpvConfStorageLocation.isBlank()) return@LaunchedEffect
              withContext(Dispatchers.IO) {
                val tempFile = kotlin.io.path.createTempFile()
                runCatching {
                  val uri = DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(mpvConfStorageLocation),
                  )!!.findFile("mpv.conf")!!.uri
                  context.contentResolver.openInputStream(uri)?.copyTo(tempFile.outputStream())
                  preferences.mpvConf.set(tempFile.readLines().fastJoinToString("\n"))
                }
                tempFile.deleteIfExists()
              }
            }
            TextFieldPreference(
              value = mpvConf,
              onValueChange = { mpvConf = it },
              title = { Text(stringResource(R.string.pref_advanced_mpv_conf)) },
              textToValue = {
                preferences.mpvConf.set(it)
                if (mpvConfStorageLocation.isNotBlank()) {
                  val tree = DocumentFile.fromTreeUri(context, Uri.parse(mpvConfStorageLocation))!!
                  val uri = if (tree.findFile("mpv.conf") == null) {
                    val conf = tree.createFile("text/plain", "mpv.conf")!!
                    conf.renameTo("mpv.conf")
                    conf.uri
                  } else {
                    tree.findFile("mpv.conf")!!.uri
                  }
                  val out = context.contentResolver.openOutputStream(uri)
                  out!!.write(it.toByteArray())
                  out.flush()
                  out.close()
                }
                it
              },
              summary = { Text(mpvConf.lines()[0]) },
            )
          }
        }
      }
    }
  }
}
