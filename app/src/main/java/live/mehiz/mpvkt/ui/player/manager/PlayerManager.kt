package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import `is`.xyz.mpv.MPV
import kotlinx.coroutines.CoroutineScope
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.di.AppGraph

class PlayerManager(
  private val mpv: MPV,
  context: Context,
  graph: AppGraph,
  scope: CoroutineScope,
) {
  val playback = PlaybackState(mpv, context, graph.playerPreferences, graph.json, scope)
  val tracks = TracksState(mpv, context, graph.json, scope)
  val subtitles = SubtitleSettings(mpv, graph.subtitlesPreferences, scope)
  val audio = AudioSettings(mpv, context, graph.audioPreferences, scope)
  val video = VideoSettings(mpv, graph.decoderPreferences, graph.advancedPreferences, graph.playerPreferences, scope)
  val lua = LuaManager(mpv, playback, graph.playerPreferences, scope)

  fun keypress(keyCode: String) {
    mpv.command("keypress", keyCode)
  }

  fun runCustomButton(button: CustomButtonEntity, longPress: Boolean = false) {
    mpv.command("script-message", "call_button_${button.id}" + if (longPress) "_long" else "")
  }
}
