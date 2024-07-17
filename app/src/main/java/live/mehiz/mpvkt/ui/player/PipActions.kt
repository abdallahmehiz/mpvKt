package live.mehiz.mpvkt.ui.player

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build.VERSION_CODES.O
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import live.mehiz.mpvkt.R

@RequiresApi(O)
fun createPipActions(
  context: Context,
  isPaused: Boolean,
): ArrayList<RemoteAction> = arrayListOf(
  createPipAction(
    context,
    "fast rewind",
    R.drawable.baseline_fast_rewind_24,
    PIP_FR,
  ),
  if (isPaused) {
    createPipAction(
      context,
      "play",
      R.drawable.baseline_play_arrow_24,
      PIP_PLAY,
    )
  } else {
    createPipAction(
      context,
      "pause",
      R.drawable.baseline_pause_24,
      PIP_PAUSE,
    )
  },
  createPipAction(
    context,
    "fast forward",
    R.drawable.baseline_fast_forward_24,
    PIP_FF
  )
)

@RequiresApi(O)
fun createPipAction(
  context: Context,
  title: String,
  @DrawableRes icon: Int,
  actionCode: Int,
): RemoteAction {
  return RemoteAction(
    Icon.createWithResource(context, icon),
    title,
    title,
    PendingIntent.getBroadcast(
      context,
      actionCode,
      Intent(PIP_INTENTS_FILTER).putExtra(PIP_INTENT_ACTION, actionCode).setPackage(context.packageName),
      PendingIntent.FLAG_IMMUTABLE,
    ),
  )
}

const val PIP_INTENTS_FILTER = "pip_control"
const val PIP_INTENT_ACTION = "media_control"
const val PIP_PAUSE = 1
const val PIP_PLAY = 2
const val PIP_FF = 3
const val PIP_FR = 4
