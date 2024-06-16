package live.mehiz.mpvkt.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import org.koin.compose.koinInject

private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  secondary = PurpleGrey80,
  tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
  primary = Purple40,
  secondary = PurpleGrey40,
  tertiary = Pink40,

  /* Other default colors to override
  background = Color(0xFFFFFBFE),
  surface = Color(0xFFFFFBFE),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFF1C1B1F),
  onSurface = Color(0xFF1C1B1F),
  */
)

@Composable
fun MpvKtTheme(content: @Composable () -> Unit) {
  val preferences = koinInject<AppearancePreferences>()
  val darkMode by preferences.darkMode.collectAsState()
  val darkTheme = isSystemInDarkTheme()
  val dynamicColor by preferences.materialYou.collectAsState()
  val context = LocalContext.current

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      when (darkMode) {
        DarkMode.Dark -> dynamicDarkColorScheme(context)
        DarkMode.Light -> dynamicLightColorScheme(context)
        else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
    }

    darkMode == DarkMode.Dark -> DarkColorScheme
    darkMode == DarkMode.Light -> LightColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
  }

  CompositionLocalProvider(
    LocalSpacing provides Spacing()
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}

enum class DarkMode(@StringRes val titleRes: Int) {
  Dark(R.string.pref_appearance_darkmode_dark),
  Light(R.string.pref_appearance_darkmode_light),
  System(R.string.pref_appearance_darkmode_system),
  ;
}

object PlayerRippleTheme : RippleTheme {

  private val alpha = RippleAlpha(
    .3f,
    .4f,
    .2f,
    .4f,
  )

  @Composable
  override fun defaultColor() = MaterialTheme.colorScheme.primaryContainer

  @Composable
  override fun rippleAlpha() = alpha
}
