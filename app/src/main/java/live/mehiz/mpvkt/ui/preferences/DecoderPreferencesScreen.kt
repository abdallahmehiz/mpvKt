package live.mehiz.mpvkt.ui.preferences

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.Debanding
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.switchPreference
import org.koin.compose.koinInject

object DecoderPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    val navigator = LocalNavigator.currentOrThrow
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_decoder))
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
        LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          switchPreference(
            preferences.tryHWDecoding.key(),
            defaultValue = preferences.tryHWDecoding.defaultValue(),
            title = { Text(stringResource(R.string.pref_decoder_try_hw_dec_title)) },
          )
          switchPreference(
            preferences.gpuNext.key(),
            defaultValue = preferences.gpuNext.defaultValue(),
            title = { Text(stringResource(R.string.pref_decoder_gpu_next_title)) },
            summary = { Text(stringResource(R.string.pref_decoder_gpu_next_summary)) },
          )
          item {
            val debanding by preferences.debanding.collectAsState()
            ListPreference(
              debanding,
              onValueChange = { preferences.debanding.set(it) },
              title = { Text(stringResource(R.string.pref_decoder_debanding_title)) },
              values = Debanding.entries,
              valueToText = { AnnotatedString(it.name) },
              summary = { Text(debanding.name) },
            )
          }
          switchPreference(
            preferences.useYUV420P.key(),
            defaultValue = preferences.useYUV420P.defaultValue(),
            title = { Text(stringResource(R.string.pref_decoder_yuv420p_title)) },
            summary = { Text(stringResource(R.string.pref_decoder_yuv420p_summary)) }
          )
        }
      }
    }
  }
}
