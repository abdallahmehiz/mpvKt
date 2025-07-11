package live.mehiz.mpvkt.ui.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import `is`.xyz.mpv.Utils
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.audioExtensions
import live.mehiz.mpvkt.ui.player.imageExtensions
import live.mehiz.mpvkt.ui.player.videoExtensions
import live.mehiz.mpvkt.ui.theme.spacing
import live.mehiz.mpvkt.ui.utils.FilesComparator
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import org.koin.compose.koinInject
import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Serializable
data class FilePickerScreen(val uri: String) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val fileManager = koinInject<FileManager>()
    val context = LocalContext.current
    val subtitlesPreferences = koinInject<SubtitlesPreferences>()
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.home_pick_file)) },
          navigationIcon = {
            IconButton(
              onClick = {
                backstack.removeAll { it is FilePickerScreen }
              },
            ) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, null)
            }
          },
        )
      },
    ) { paddingValues ->
      FilePicker(
        directory = fileManager.fromUri(uri.toUri())!!,
        onNavigate = { newFile ->
          if (fileManager.isFile(newFile)) {
            if (subtitlesPreferences.autoLoadExternal.get()) {
              val videoNameWithoutExt = fileManager.getName(newFile).substringBeforeLast(".")
              val parentDir = fileManager.fromUri(uri.toUri())!!
              val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt", "sub")
              val subtitlePaths = fileManager.listFiles(parentDir).filter { potentialSubFile ->
                if (fileManager.isDirectory(potentialSubFile)) false
                else {
                  val subFileName = fileManager.getName(potentialSubFile)
                  val subFileNameWithoutExt = subFileName.substringBeforeLast('.')
                  val subFileExt = subFileName.substringAfterLast('.').lowercase()
                  // Matching rule: File names have the same prefix and the extension is a subtitle format
                  subFileNameWithoutExt.startsWith(videoNameWithoutExt) && subFileExt in subtitleExtensions
                }
              }.map { it.getFullPath() }

              playFileWithSubtitles(newFile.getFullPath(), subtitlePaths, context)
            } else {
              HomeScreen.playFile(newFile.getFullPath(), context)
            }
            return@FilePicker
          }
          backstack.add(FilePickerScreen(newFile.getFullPath()))
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
    val navigator = LocalBackStack.current
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
          onClick = { navigator.removeLastOrNull() },
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
          items = if (fileManager.isDirectory(file)) fileManager.listFiles(file).size else null,
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
    items: Int? = null,
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
        .padding(vertical = MaterialTheme.spacing.smaller, horizontal = MaterialTheme.spacing.medium),
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
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
          if (size != null || items != null) {
            Text(
              text = if (isDirectory) {
                pluralStringResource(
                  id = R.plurals.plural_items,
                  count = items!!,
                  items,
                )
              } else {
                size!!
              },
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

  fun playFileWithSubtitles(
    filepath: String,
    subtitlePaths: List<String>,
    context: Context,
  ) {
    val i = Intent(Intent.ACTION_VIEW, filepath.toUri())
    i.setClass(context, PlayerActivity::class.java)
    if (subtitlePaths.isNotEmpty()) {
      val subtitleUris = subtitlePaths.map { it.toUri() }.toTypedArray()
      i.putExtra("subs", subtitleUris)
      i.putExtra("subs.enable", arrayOf(subtitleUris.first()))
    }
    context.startActivity(i)
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
}
