package live.mehiz.mpvkt.ui.utils

import androidx.compose.runtime.compositionLocalOf
import live.mehiz.mpvkt.presentation.Screen

@Suppress("CompositionLocalAllowlist")
val LocalBackStack = compositionLocalOf<MutableList<Screen>> {
  error("LocalBackStack not initialized!")
}
