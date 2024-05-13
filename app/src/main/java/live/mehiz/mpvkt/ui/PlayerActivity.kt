package live.mehiz.mpvkt.ui

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
        val binding = PlayerLayoutBinding.inflate(this.layoutInflater)
        setContentView(binding.root)
        MPVLib.create(this, "v")
        binding.playerView.initialize(
            applicationContext.filesDir.path,
            applicationContext.cacheDir.path
        )
        val uri = intent.extras!!.getString("uri")!!
        val videoUri = if (uri.startsWith("content://")) {
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