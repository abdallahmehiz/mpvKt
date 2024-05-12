package live.mehiz.mpvkt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import live.mehiz.mpvkt.ui.PlayerActivity
import live.mehiz.mpvkt.ui.theme.MpvKtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MpvKtTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var uri by remember { mutableStateOf("") }
                    TextField(value = uri, onValueChange = { uri = it })
                    Button(onClick = {
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                        intent.putExtra("uri", uri)
                        this@MainActivity.startActivity(intent)
                    }) {
                        Text(text = "Start playing?")
                    }
                }
            }
        }
    }
}