package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.Screen
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.DecoderPreferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.switchPreference
import org.koin.compose.koinInject

object DecoderPreferencesScreen: Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_decoder))
          }
        )
      }
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
          switchPreference(
            preferences.tryHWDecoding.key(),
            defaultValue = preferences.tryHWDecoding.defaultValue(),
            title = { Text(stringResource(R.string.pref_decoder_try_hw_dec_title)) }
          )
        }
      }
    }
  }
}
