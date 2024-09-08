package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.Debanding
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

object DecoderPreferencesScreen : Screen() {
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
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          val tryHWDecoding by preferences.tryHWDecoding.collectAsState()
          SwitchPreference(
            value = tryHWDecoding,
            onValueChange = {
              preferences.tryHWDecoding.set(it)
            },
            title = { Text(stringResource(R.string.pref_decoder_try_hw_dec_title)) },
          )
          val gpuNext by preferences.gpuNext.collectAsState()
          SwitchPreference(
            value = gpuNext,
            onValueChange = {
              preferences.gpuNext.set(it)
            },
            title = { Text(stringResource(R.string.pref_decoder_gpu_next_title)) },
            summary = { Text(stringResource(R.string.pref_decoder_gpu_next_summary)) },
          )
          val debanding by preferences.debanding.collectAsState()
          ListPreference(
            value = debanding,
            onValueChange = { preferences.debanding.set(it) },
            values = Debanding.entries,
            title = { Text(stringResource(R.string.pref_decoder_debanding_title)) },
            summary = { Text(debanding.name) }
          )
          val useYUV420p by preferences.useYUV420P.collectAsState()
          SwitchPreference(
            value = useYUV420p,
            onValueChange = {
              preferences.useYUV420P.set(it)
            },
            title = { Text(stringResource(R.string.pref_decoder_yuv420p_title)) },
            summary = { Text(stringResource(R.string.pref_decoder_yuv420p_summary)) }
          )
        }
      }
    }
  }
}
