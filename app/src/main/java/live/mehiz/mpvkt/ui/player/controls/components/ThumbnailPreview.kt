package live.mehiz.mpvkt.ui.player.controls.components

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import live.mehiz.mpvkt.ui.player.manager.Thumbnailer
import live.mehiz.mpvkt.ui.theme.spacing

private val PreviewWidth = 160.dp

// same as the timers in SeekbarWithTimers
private val TimerWidth = 92.dp

// Heavily modified version of https://github.com/animiruorg/Animiru/blob/01f551864b6ae0f9b3228318ce10a5b262c20640/app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/ThumbnailPreview.kt
@Composable
fun ThumbnailPreview(
  thumbnailer: Thumbnailer,
  visible: Boolean,
  aspect: Float?,
  position: Float,
  duration: Float,
  modifier: Modifier = Modifier,
) {
  val timerSpace = TimerWidth + MaterialTheme.spacing.extraSmall
  val alpha by animateFloatAsState(if (visible && aspect != null) 1f else 0f, label = "thumbnail_alpha")
  BoxWithConstraints(modifier) {
    val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val center = timerSpace + (maxWidth - timerSpace * 2) * progress
    val shape = RoundedCornerShape(8.dp)
    AndroidView(
      factory = { context ->
        TextureView(context).apply {
          surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            private var surface: Surface? = null

            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
              surface = Surface(texture).also { thumbnailer.attachSurface(it, width, height) }
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) =
              thumbnailer.resizeSurface(width, height)

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
              thumbnailer.detachSurface()
              surface?.release()
              surface = null
              return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
          }
        }
      },
      modifier = Modifier
        .offset(x = (center - PreviewWidth / 2).coerceIn(0.dp, maxWidth - PreviewWidth))
        .width(PreviewWidth)
        .aspectRatio(aspect ?: (16f / 9f))
        .alpha(alpha)
        .clip(shape)
        .border(1.dp, Color.White.copy(alpha = .5f), shape),
    )
  }
}
