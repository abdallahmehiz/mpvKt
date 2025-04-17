package live.mehiz.mpvkt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsScreen
import live.mehiz.mpvkt.ui.home.FilePickerScreen
import live.mehiz.mpvkt.ui.home.HomeScreen
import live.mehiz.mpvkt.ui.preferences.AboutScreen
import live.mehiz.mpvkt.ui.preferences.AdvancedPreferencesScreen
import live.mehiz.mpvkt.ui.preferences.AppearancePreferencesScreen
import live.mehiz.mpvkt.ui.preferences.AudioPreferencesScreen
import live.mehiz.mpvkt.ui.preferences.DecoderPreferencesScreen
import live.mehiz.mpvkt.ui.preferences.GesturePreferencesScreen
import live.mehiz.mpvkt.ui.preferences.LibrariesScreen
import live.mehiz.mpvkt.ui.preferences.PlayerPreferencesScreen
import live.mehiz.mpvkt.ui.preferences.PreferencesScreen
import live.mehiz.mpvkt.ui.preferences.SubtitlesPreferencesScreen
import live.mehiz.mpvkt.ui.theme.DarkMode
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import live.mehiz.mpvkt.ui.utils.LocalNavController
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      enableEdgeToEdge(
        SystemBarStyle.auto(
          lightScrim = Color.White.toArgb(),
          darkScrim = Color.White.toArgb(),
        ) { dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme) },
      )
      MpvKtTheme { Navigator() }
    }
  }

  @Composable
  fun Navigator() {
    CompositionLocalProvider(LocalNavController provides rememberNavController()) {
      NavHost(
        LocalNavController.current,
        startDestination = HomeScreen,
        enterTransition = {
          fadeIn(animationSpec = tween(220)) +
            slideIn(animationSpec = tween(220)) { IntOffset(it.width / 2, 0) }
        },
        exitTransition = {
          fadeOut(animationSpec = tween(220)) +
            slideOut(animationSpec = tween(220)) { IntOffset(-it.width / 2, 0) }
        },
        popEnterTransition = {
          fadeIn(animationSpec = tween(220)) +
            scaleIn(animationSpec = tween(220, delayMillis = 30), initialScale = .9f, TransformOrigin(-1f, .5f))
        },
        popExitTransition = {
          fadeOut(animationSpec = tween(220)) +
            scaleOut(animationSpec = tween(220, delayMillis = 30), targetScale = .9f, TransformOrigin(-1f, .5f))
        },
      ) {
        composable<HomeScreen> { HomeScreen.Content() }
        composable<FilePickerScreen> { it.toRoute<FilePickerScreen>().Content() }

        composable<AboutScreen> { AboutScreen.Content() }
        composable<AdvancedPreferencesScreen> { AdvancedPreferencesScreen.Content() }
        composable<AppearancePreferencesScreen> { AppearancePreferencesScreen.Content() }
        composable<AudioPreferencesScreen> { AudioPreferencesScreen.Content() }
        composable<CustomButtonsScreen> { CustomButtonsScreen.Content() }
        composable<DecoderPreferencesScreen> { DecoderPreferencesScreen.Content() }
        composable<GesturePreferencesScreen> { GesturePreferencesScreen.Content() }
        composable<LibrariesScreen> { LibrariesScreen.Content() }
        composable<PlayerPreferencesScreen> { PlayerPreferencesScreen.Content() }
        composable<PreferencesScreen> { PreferencesScreen.Content() }
        composable<SubtitlesPreferencesScreen> { SubtitlesPreferencesScreen.Content() }
      }
    }
  }
}
