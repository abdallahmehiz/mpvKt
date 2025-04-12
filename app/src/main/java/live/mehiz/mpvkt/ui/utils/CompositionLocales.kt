package live.mehiz.mpvkt.ui.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

@Suppress("CompositionLocalAllowlist")
val LocalNavController = compositionLocalOf<NavHostController> {
  error("LocalNavController not initialized!")
}
