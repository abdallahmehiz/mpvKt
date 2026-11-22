package live.mehiz.mpvkt.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

fun hasAllFilesAccess(): Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

enum class MediaAccess {
  Full,

  // A14+
  // TODO: let the user ask for full access from here, right now partial access has no way out
  Partial,
  Denied,
}

private val mediaPermissions: Array<String> = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
  )

  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.READ_MEDIA_IMAGES,
  )

  else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.mediaAccess(): MediaAccess {
  fun granted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
  return when {
    hasAllFilesAccess() -> MediaAccess.Full

    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
      if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) MediaAccess.Full else MediaAccess.Denied

    // TODO: tell the user when some media types aren't granted, their files silently don't show up
    granted(Manifest.permission.READ_MEDIA_VIDEO) ||
      granted(Manifest.permission.READ_MEDIA_AUDIO) ||
      granted(Manifest.permission.READ_MEDIA_IMAGES) -> MediaAccess.Full

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
      granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.Partial

    else -> MediaAccess.Denied
  }
}

class MediaAccessState {
  var access by mutableStateOf(MediaAccess.Denied)
  var request: () -> Unit = {}
}

@Composable
fun rememberMediaAccess(onChange: () -> Unit): MediaAccessState {
  val context = LocalContext.current
  val activity = LocalActivity.current
  var askedBefore by rememberSaveable { mutableStateOf(false) }
  val state = remember { MediaAccessState().apply { access = context.mediaAccess() } }

  fun update() {
    val now = context.mediaAccess()
    if (now != state.access) {
      state.access = now
      onChange()
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    askedBefore = true
    update()
  }
  val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    update()
  }
  state.request = {
    val blocked = askedBefore && activity != null && state.access == MediaAccess.Denied &&
      mediaPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    if (blocked) {
      settingsLauncher.launch(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
      )
    } else {
      permissionLauncher.launch(mediaPermissions)
    }
  }
  return state
}
