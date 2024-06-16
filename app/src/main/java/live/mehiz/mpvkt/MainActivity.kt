package live.mehiz.mpvkt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.enableLiveLiterals
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.home.HomeScreen
import live.mehiz.mpvkt.ui.theme.DarkMode
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.createScope
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject

class MainActivity : ComponentActivity() {
  val appearancePreferences by inject<AppearancePreferences>()
  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      enableEdgeToEdge(
        SystemBarStyle.auto(
          0xFFF,
          0xFFF
        ) { dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme) },
      )
      MpvKtTheme {
        // TODO: add transitions back once these two issues get fixed, thanks Google, very cool!
        // https://github.com/adrielcafe/voyager/issues/410
        // https://github.com/adrielcafe/voyager/issues/431
        Navigator(HomeScreen)
      }
    }
  }
}
