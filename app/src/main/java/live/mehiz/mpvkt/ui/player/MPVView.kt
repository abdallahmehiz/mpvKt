package live.mehiz.mpvkt.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import `is`.xyz.mpv.KeyMapping

@SuppressLint("ViewConstructor")
class MPVView(context: Context, private val service: MediaPlaybackService) :
  SurfaceView(context),
  SurfaceHolder.Callback {

  val mpv
    get() = service.mpv

  override fun surfaceCreated(holder: SurfaceHolder) = service.attachSurface(holder.surface)

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
    service.resizeSurface(width, height)

  override fun surfaceDestroyed(holder: SurfaceHolder) = service.detachSurface()

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    holder.addCallback(this)
  }

  override fun onDetachedFromWindow() {
    holder.removeCallback(this)
    super.onDetachedFromWindow()
  }

  /**
   * Returns the video aspect ratio. Rotation is taken into account.
   */
  fun getVideoOutAspect(): Double? {
    return mpv.getPropertyDouble("video-params/aspect")?.let {
      if (it < 0.001) return 0.0
      if ((mpv.getPropertyInt("video-params/rotate") ?: 0) % 180 == 90) 1.0 / it else it
    }
  }

  @Suppress("ReturnCount")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        if (event.repeatCount == 0) {
          Log.d(TAG, "Unmapped non-printable key ${event.keyCode}")
        }
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true // eat event but ignore it, mpv has its own key repeat
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    mpv.command(action, mod.joinToString("+"))

    return true
  }
}
