package live.mehiz.mpvkt.ui.preferences

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Github
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.BuildConfig
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import live.mehiz.mpvkt.ui.theme.spacing
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals

@Serializable
object AboutScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val backstack = LocalBackStack.current
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.pref_about_title)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .padding(paddingValues)
          .verticalScroll(rememberScrollState()),
      ) {
        Column(
          Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.large),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Image(
            painterResource(id = R.drawable.ic_launcher_foreground),
            null,
            modifier = Modifier.size(160.dp),
          )
          Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
          )
        }
        HorizontalDivider()
        ProvidePreferenceLocals {
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_app_version)) },
            summary = {
              Text(
                text = stringResource(
                  id = R.string.pref_about_app_version_formatted,
                  BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercaseChar() },
                  BuildConfig.VERSION_NAME,
                ),
              )
            },
            onClick = {
              clipboard.setText(AnnotatedString(collectDeviceInfo()))
            },
          )
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_oss_libraries)) },
            onClick = { backstack.add(LibrariesScreen) },
          )
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_privacy_policy)) },
            onClick = {
              context.startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  context.getString(R.string.privacy_policy_url).toUri(),
                ),
              )
            },
          )
        }
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          IconButton(
            onClick = {
              context.startActivity(
                Intent(Intent.ACTION_VIEW, context.getString(R.string.github_repo_url).toUri()),
              )
            },
          ) {
            Icon(imageVector = SimpleIcons.Github, contentDescription = null)
          }
        }
      }
    }
  }
}

@Serializable
object LibrariesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(R.string.pref_about_oss_libraries)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { paddingValues ->
      LibrariesContainer(modifier = Modifier.padding(paddingValues))
    }
  }
}
