package live.mehiz.mpvkt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cafe.adriel.voyager.navigator.Navigator
import live.mehiz.mpvkt.ui.home.HomeScreen
import live.mehiz.mpvkt.ui.theme.MpvKtTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MpvKtTheme {
        // TODO: add transitions back once these two issues get fixed, thanks Google, very cool!
        // https://github.com/adrielcafe/voyager/issues/410
        // https://github.com/adrielcafe/voyager/issues/431
        Navigator(HomeScreen)
      }
    }
  }
}
