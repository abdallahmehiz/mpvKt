package live.mehiz.mpvkt.ui.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Suppress("CompositionLocalAllowlist")
val LocalBackStack = compositionLocalOf<NavBackStack<NavKey>> {
  error("LocalBackStack not initialized!")
}
