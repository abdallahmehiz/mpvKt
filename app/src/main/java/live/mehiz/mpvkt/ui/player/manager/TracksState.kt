package live.mehiz.mpvkt.ui.player.manager

import live.mehiz.mpvkt.ui.player.mapState
import live.mehiz.mpvkt.ui.player.toObject
import android.content.Context
import android.net.Uri
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.player.openContentFd

class TracksState(
  private val mpv: MPV,
  private val context: Context,
  json: Json,
  scope: CoroutineScope,
) {
  private val trackList: StateFlow<List<TrackNode>> = mpv.propFlow<MPVNode>("track-list")
    .mapState(scope, { emptyList() }) { it.toObject<List<TrackNode>>(json) }

  val subtitles: StateFlow<ImmutableList<TrackNode>> =
    trackList.mapState(scope, { persistentListOf() }) { list -> list.filter { it.isSubtitle }.toImmutableList() }
  val audio: StateFlow<ImmutableList<TrackNode>> =
    trackList.mapState(scope, { persistentListOf() }) { list -> list.filter { it.isAudio }.toImmutableList() }

  fun selectSubtitle(id: Int) {
    val sid = mpv.getPropertyInt("sid")
    val secondarySid = mpv.getPropertyInt("secondary-sid")
    val (primary, secondary) = when (id) {
      sid -> secondarySid to null
      secondarySid -> sid to null
      else -> if (sid != null) sid to id else id to null
    }
    primary?.let { mpv.prop("sid", it) } ?: mpv.prop("sid", false)
    secondary?.let { mpv.prop("secondary-sid", it) } ?: mpv.prop("secondary-sid", false)
  }

  fun toggleAudio(id: Int) {
    if (id == mpv.getPropertyInt("aid")) mpv.prop("aid", false) else mpv.prop("aid", id)
  }

  fun addSubtitle(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) uri.openContentFd(context) ?: url else url
    mpv.command("sub-add", path, "cached")
  }

  fun addAudio(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) uri.openContentFd(context) else url
    mpv.command("audio-add", path ?: return, "cached")
  }
}
