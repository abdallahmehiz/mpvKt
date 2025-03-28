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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import hendrawd.storageutil.library.StorageUtil
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.audioExtensions
import live.mehiz.mpvkt.ui.player.imageExtensions
import live.mehiz.mpvkt.ui.player.videoExtensions
import live.mehiz.mpvkt.ui.theme.spacing
import live.mehiz.mpvkt.ui.utils.FilesComparator
import java.io.File
import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class FilePickerScreen(val path: String? = null) : Screen() {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
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
      if (path == null) {
        StoragePicker(
          onNavigate = { device -> navigator.push(FilePickerScreen(device.absolutePath)) },
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      } else {
        FilePicker(
          directory = File(path),
          onNavigate = { newFile ->
            if (newFile.isFile) {
              HomeScreen.playFile(Uri.fromFile(newFile).toString(), context)
              return@FilePicker
            }
            navigator.push(FilePickerScreen(newFile.absolutePath))
          },
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      }
    }
  }

  @Composable
  fun StoragePicker(
    modifier: Modifier = Modifier,
    onNavigate: (File) -> Unit,
  ) {
    val context = LocalContext.current
    val deviceList = StorageUtil.getStorageDirectories(context)
      .map { File(it) }
      .filter { it.exists() }

    LazyColumn(modifier) {
      itemsIndexed(deviceList, key = { _, file -> file.absolutePath }) { index, file ->
        FileListing(
          name = file.absolutePath,
          isDirectory = true,
          lastModified = null,
          length = null,
          modifier = Modifier.background(
            if (index % 2 == 0) {
              MaterialTheme.colorScheme.surfaceContainerLow
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
          ),
          items = null,
          onClick = { onNavigate(file) },
        )
      }
    }
  }

  @Composable
  fun FilePicker(
    directory: File,
    modifier: Modifier = Modifier,
    onNavigate: (File) -> Unit,
  ) {
    val navigator = LocalNavigator.currentOrThrow
    val fileList = directory.listFiles { file ->
      val name = file.name
      if (name.startsWith('.')) return@listFiles false
      if (file.isDirectory) return@listFiles true
      file.isFile && Utils.MEDIA_EXTENSIONS.contains(name.substringAfterLast('.').lowercase(Locale.ENGLISH))
    }?.sortedWith(FilesComparator()) ?: emptyList()

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
      itemsIndexed(fileList, key = { _, file -> file.name }) { index, file ->
        FileListing(
          name = file.name,
          isDirectory = file.isDirectory,
          lastModified = file.lastModified(),
          length = if (file.isFile) file.length() else null,
          modifier = Modifier.background(
            if (index % 2 == 1) {
              MaterialTheme.colorScheme.surfaceContainerLow
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
          ),
          items = if (file.isDirectory) file.listFiles()?.size else null,
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
