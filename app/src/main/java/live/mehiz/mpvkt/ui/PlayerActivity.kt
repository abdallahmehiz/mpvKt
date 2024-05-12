package live.mehiz.mpvkt.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding

class PlayerActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = PlayerLayoutBinding.inflate(this.layoutInflater)
        setContentView(binding.root)
        MPVLib.create(this, "v")
        binding.playerView.initialize(applicationContext.filesDir.path, applicationContext.cacheDir.path)
        binding.playerView.playFile(intent.extras!!.getString("uri")!!)
    }

    override fun onDestroy() {
        MPVLib.destroy()
        super.onDestroy()
    }
}