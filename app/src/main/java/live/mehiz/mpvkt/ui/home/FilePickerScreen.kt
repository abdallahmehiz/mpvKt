package live.mehiz.mpvkt.ui.home

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.utils.FilesComparator
import org.koin.compose.koinInject
import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class FilePickerScreen(val uri: String) : Screen() {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val fileManager = koinInject<FileManager>()
    val context = LocalContext.current
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.home_pick_file)) },
          navigationIcon = {
            IconButton(onClick = { navigator.replaceAll(HomeScreen) }) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, null)
            }
          },
        )
      },
    ) { paddingValues ->
      FilePicker(
        directory = fileManager.fromUri(Uri.parse(uri))!!,
        onNavigate = { newFile ->
          if (fileManager.isFile(newFile)) {
            HomeScreen.playFile(newFile.getFullPath(), context)
            return@FilePicker
          }
          navigator.push(FilePickerScreen(newFile.getFullPath()))
        },
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      )
    }
  }

  @Composable
  fun FilePicker(
    directory: AbstractFile,
    modifier: Modifier = Modifier,
    onNavigate: (AbstractFile) -> Unit,
  ) {
    val navigator = LocalNavigator.currentOrThrow
    val fileManager = koinInject<FileManager>()
    val fileList = fileManager.listFiles(directory).filterNot {
      !Utils.MEDIA_EXTENSIONS.contains(fileManager.getName(it).substringAfterLast('.')) &&
        fileManager.isFile(it) || fileManager.getName(it).startsWith('.')
    }.sortedWith(FilesComparator(fileManager))

    LazyColumn(modifier) {
      item {
        FileListing(
          name = "..",
          isDirectory = true,
          lastModified = null,
          length = 0L,
          onClick = { navigator.pop() },
          modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
        )
      }
      itemsIndexed(fileList, key = { _, file -> fileManager.getName(file) }) { index, file ->
        FileListing(
          name = fileManager.getName(file),
          isDirectory = fileManager.isDirectory(file),
          lastModified = fileManager.lastModified(file),
          length = if (fileManager.isFile(file)) fileManager.getLength(file) else null,
          modifier = Modifier.background(
            if (index % 2 == 1) {
              MaterialTheme.colorScheme.surfaceContainerLow
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
          ),
          onClick = { onNavigate(file) },
        )
      }
    }
  }

  @Composable
  fun FileListing(
    name: String,
    isDirectory: Boolean,
    lastModified: Long?,
    length: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
  ) {
    var size: String? by remember { mutableStateOf(null) }
    var time: String? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        lastModified?.let {
          time = Instant.ofEpochMilli(lastModified).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss"))
        }
      }
      if (isDirectory) return@LaunchedEffect
      length?.let { size = it.asHumanReadableByteCountBin() }
    }
    Row(
      modifier = modifier
        .clickable(onClick = onClick)
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .padding(vertical = 8.dp, horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = fileIcon(isDirectory = isDirectory, fileExtension = name.substringAfterLast('.')),
        contentDescription = null,
      )
      Column {
        Text(
          text = name,
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyLarge,
        )
        if (isDirectory && lastModified == null) return@Column
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = time ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
          )
          if (size != null && !isDirectory) {
            Text(
              text = size!!,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    }
  }

  @Composable
  fun fileIcon(
    isDirectory: Boolean,
    fileExtension: String,
  ): ImageVector {
    if (isDirectory) return Icons.Filled.Folder
    return when (fileExtension) {
      in videoExtensions -> Icons.Filled.Movie
      in audioExtensions -> Icons.Filled.Audiotrack
      in imageExtensions -> Icons.Filled.Image
      else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
  }

  private fun Long.asHumanReadableByteCountBin(): String {
    val absB = if (this == Long.MIN_VALUE) Long.MAX_VALUE else abs(this)
    if (absB < 1024) return "$this B"
    var value = absB
    val units = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
      value = value shr 10
      units.next()
      i -= 10
    }
    value *= signum(this)
    return String.format(
      locale = java.util.Locale.US,
      format = "%.1f %ciB",
      value / 1024.0,
      units.current(),
    )
  }

  private val videoExtensions = listOf(
    "264", "265", "3g2", "3ga", "3gp", "3gp2", "3gpp", "3gpp2", "3iv", "amr", "asf",
    "asx", "av1", "avc", "avf", "avi", "bdm", "bdmv", "clpi", "cpi", "divx", "dv", "evo",
    "evob", "f4v", "flc", "fli", "flic", "flv", "gxf", "h264", "h265", "hdmov", "hdv",
    "hevc", "lrv", "m1u", "m1v", "m2t", "m2ts", "m2v", "m4u", "m4v", "mkv", "mod", "moov",
    "mov", "mp2", "mp2v", "mp4", "mp4v", "mpe", "mpeg", "mpeg2", "mpeg4", "mpg", "mpg4",
    "mpl", "mpls", "mpv", "mpv2", "mts", "mtv", "mxf", "mxu", "nsv", "nut", "ogg", "ogm",
    "ogv", "ogx", "qt", "qtvr", "rm", "rmj", "rmm", "rms", "rmvb", "rmx", "rv", "rvx",
    "sdp", "tod", "trp", "ts", "tsa", "tsv", "tts", "vc1", "vfw", "vob", "vro", "webm",
    "wm", "wmv", "wmx", "x264", "x265", "xvid", "y4m", "yuv",
  )

  private val audioExtensions = listOf(
    "3ga", "3ga2", "a52", "aac", "ac3", "adt", "adts", "aif", "aifc", "aiff", "alac",
    "amr", "ape", "au", "awb", "dsf", "dts", "dts-hd", "dtshd", "eac3", "f4a", "flac",
    "lpcm", "m1a", "m2a", "m4a", "mk3d", "mka", "mlp", "mp+", "mp1", "mp2", "mp3", "mpa",
    "mpc", "mpga", "mpp", "oga", "ogg", "opus", "pcm", "ra", "ram", "rax", "shn", "snd",
    "spx", "tak", "thd", "thd+ac3", "true-hd", "truehd", "tta", "wav", "weba", "wma", "wv",
    "wvp",
  )

  private val imageExtensions = listOf(
    "apng", "bmp", "exr", "gif", "j2c", "j2k", "jfif", "jp2", "jpc", "jpe", "jpeg", "jpg",
    "jpg2", "png", "tga", "tif", "tiff", "webp",
  )
}
