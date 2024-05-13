package live.mehiz.mpvkt.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import java.io.File

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.extras?.getString("uri")?.isNotBlank() == true) onDestroy()
        val binding = PlayerLayoutBinding.inflate(this.layoutInflater)
        setContentView(binding.root)
        MPVLib.create(this, "v")
        binding.playerView.initialize(
            applicationContext.filesDir.path,
            applicationContext.cacheDir.path
        )
        val uri = parsePathFromIntent(intent)
        val videoUri = if (uri?.startsWith("content://") == true) {
            openContentFd(Uri.parse(uri))
        } else {
            uri
        }
        binding.playerView.playFile(videoUri!!)
    }

    override fun onDestroy() {
        MPVLib.destroy()
        super.onDestroy()
    }

    private fun parsePathFromIntent(intent: Intent): String? {
        val filepath: String? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { resolveUri(it) }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                val uri = Uri.parse(it.trim())
                if (uri.isHierarchical && !uri.isRelative) resolveUri(uri) else null
            }
            else -> intent.getStringExtra("uri")
        }
        return filepath
    }

    private fun resolveUri(data: Uri): String? {
        val filepath = when (data.scheme) {
            "file" -> data.path
            "content" -> openContentFd(data)
            "http", "https", "rtmp", "rtmps", "rtp", "rtsp", "mms", "mmst", "mmsh", "tcp", "udp", "lavf"
            -> data.toString()
            else -> null
        }

        if (filepath == null)
            Log.e("mpvKt", "unknown scheme: ${data.scheme}")
        return filepath
    }

    private fun openContentFd(uri: Uri): String? {
        if (uri.scheme != "content") return null
        val resolver = applicationContext.contentResolver
        Log.d("mpvKt", "Resolving content URI: $uri")
        val fd = try {
            val desc = resolver.openFileDescriptor(uri, "r")
            desc!!.detachFd()
        } catch (e: Exception) {
            Log.d("mpvKt", "Failed to open content fd: $e")
            return null
        }
        try {
            val path = File("/proc/self/fd/$fd").canonicalPath
            if (!path.startsWith("/proc") && File(path).canRead()) {
                Log.d("mpvKt", "Found real file path: $path")
                ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
                return path
            }
        } catch (_: Exception) {

        }
        // Else, pass the fd to mpv
        return "fdclose://$fd"
    }
}