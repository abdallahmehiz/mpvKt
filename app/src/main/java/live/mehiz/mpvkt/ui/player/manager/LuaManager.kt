package live.mehiz.mpvkt.ui.player.manager

import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import live.mehiz.mpvkt.preferences.PlayerPreferences

/**
 * mpvkt.lua talks to the app by writing user-data properties. Listens for as long as mpv lives,
 * so playback commands work without a UI, and clears each property so the same command can repeat
 * this'd avoid lua events being ignored/not ran by the activity when it's destroyed or something
 */
class LuaManager(
  private val mpv: MPV,
  private val playback: PlaybackState,
  private val preferences: PlayerPreferences,
  scope: CoroutineScope,
) {
  sealed interface Event {
    data class Ui(val command: String, val data: String) : Event
    data class Seek(val offset: Int, val absolute: Boolean, val text: String?) : Event
  }

  private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
  val events = _events.asSharedFlow()

  init {
    COMMANDS.forEach { command ->
      val property = "user-data/mpvkt/$command"
      mpv.propFlow<String>(property)
        .onEach { value ->
          val data = value?.removePrefix("\"")?.removeSuffix("\"")
          if (data.isNullOrEmpty()) return@onEach
          handle(command, data)
          mpv.setPropertyString(property, "")
        }
        .launchIn(scope)
    }
  }

  private fun handle(command: String, data: String) {
    when (command) {
      "seek_by", "seek_by_with_text" -> {
        val (offset, text) = parseSeek(data)
        playback.seekBy(offset, preferences.preciseSeeking.get())
        _events.tryEmit(Event.Seek(offset, absolute = false, text))
      }

      "seek_to", "seek_to_with_text" -> {
        val (position, text) = parseSeek(data)
        val offset = position - playback.position.value
        playback.seekTo(position, preferences.preciseSeeking.get())
        _events.tryEmit(Event.Seek(offset, absolute = true, text))
      }

      else -> _events.tryEmit(Event.Ui(command, data))
    }
  }

  private fun parseSeek(data: String): Pair<Int, String?> {
    val parts = data.split("|", limit = 2)
    return parts[0].toInt() to parts.getOrNull(1)
  }

  private companion object {
    val COMMANDS = listOf(
      "show_text",
      "toggle_ui",
      "show_panel",
      "set_button_title",
      "reset_button_title",
      "toggle_button",
      "seek_by",
      "seek_to",
      "seek_by_with_text",
      "seek_to_with_text",
      "software_keyboard",
    )
  }
}
