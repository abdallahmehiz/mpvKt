package live.mehiz.mpvkt.ui.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import live.mehiz.mpvkt.domain.media.SortField
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.paging.LoadState
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.data.media.contentUri
import live.mehiz.mpvkt.domain.media.MediaFile
import live.mehiz.mpvkt.domain.media.MediaQuery
import live.mehiz.mpvkt.domain.media.MediaType
import live.mehiz.mpvkt.domain.media.childFolders
import live.mehiz.mpvkt.domain.media.sortedBy
import live.mehiz.mpvkt.domain.media.volumeFolders
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.audioExtensions
import live.mehiz.mpvkt.ui.player.imageExtensions
import live.mehiz.mpvkt.ui.player.videoExtensions
import live.mehiz.mpvkt.ui.theme.spacing
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Serializable
data class FilePickerScreenMediaStore(val path: String? = null) : Screen {

  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val repository = App.graph.mediaRepository
    val access = rememberMediaAccess(onChange = repository::refresh)
    // null while the search field is closed
    var search: String? by rememberSaveable { mutableStateOf(null) }
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            val query = search
            if (query == null) {
              Text(text = stringResource(id = R.string.home_pick_file))
            } else {
              val focusRequester = remember { FocusRequester() }
              TextField(
                value = query,
                onValueChange = { search = it },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
              )
              LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
          },
          actions = {
            if (search == null) {
              IconButton(onClick = { search = "" }) { Icon(Icons.Default.Search, null) }
            } else {
              IconButton(onClick = { search = null }) { Icon(Icons.Default.Close, null) }
            }
            BrowserActions()
          },
          navigationIcon = {
            IconButton(
              onClick = {
                backstack.removeAll { it is FilePickerScreenMediaStore }
              },
            ) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, null)
            }
          },
        )
      },
    ) { paddingValues ->
      val modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
      if (access.access == MediaAccess.Denied) {
        Box(modifier, contentAlignment = Alignment.Center) {
          Button(onClick = { access.request() }) { Text(stringResource(R.string.media_access_grant)) }
        }
        return@Scaffold
      }
      FilePicker(
        onOpenDirectory = { backstack.add(FilePickerScreenMediaStore(it)) },
        onOpenFile = { HomeScreen.playFile(it.contentUri().toString(), context) },
        search = search.orEmpty(),
        modifier = modifier,
      )
    }
  }

  @Composable
  fun FilePicker(
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (MediaFile) -> Unit,
    search: String,
    modifier: Modifier = Modifier,
  ) {
    val navigator = LocalBackStack.current
    val repository = App.graph.mediaRepository
    val browserPreferences = App.graph.fileManagerPreferences
    val sortField by browserPreferences.sortField.collectAsState()
    val sortAscending by browserPreferences.sortAscending.collectAsState()
    val typeNames by browserPreferences.mediaTypes.collectAsState()
    val types = remember(typeNames) {
      typeNames.mapNotNull { name -> MediaType.entries.find { it.name == name } }.toSet()
    }

    val stats by remember(types) { repository.observeDirectories(types) }.collectAsState(emptyMap())
    val folders = remember(stats, sortField, sortAscending, search) {
      (if (path == null) volumeFolders(stats) else childFolders(stats, path))
        .filter { it.name.contains(search.trim(), ignoreCase = true) }
        .sortedBy(sortField, sortAscending)
    }
    val files = remember(types, sortField, sortAscending, search) {
      if (path == null) {
        emptyFlow()
      } else {
        Pager(PagingConfig(pageSize = 50)) {
          repository.pagingSource(MediaQuery(types, sortField, sortAscending, path, recursive = false, search))
        }.flow
      }
    }.collectAsLazyPagingItems()
    LaunchedEffect(files) { repository.changes.collect { files.refresh() } }

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
      itemsIndexed(folders, key = { _, folder -> folder.path }) { index, folder ->
        FileListing(
          name = folder.name,
          isDirectory = true,
          lastModified = folder.stats.dateModified * 1000,
          length = null,
          modifier = Modifier.background(rowColor(index)),
          items = folder.stats.count,
          onClick = { onOpenDirectory(folder.path) },
        )
      }
      if (files.loadState.refresh is LoadState.Error) {
        item {
          Text(
            text = "Couldn't read media files",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
          )
        }
      }
      if (path != null) {
        items(files.itemCount, key = files.itemKey { it.id }) { index ->
          val file = files[index] ?: return@items
          FileListing(
            name = file.name,
            isDirectory = false,
            lastModified = file.dateModified * 1000,
            length = file.size,
            modifier = Modifier.background(rowColor(folders.size + index)),
            onClick = { onOpenFile(file) },
          )
        }
      }
    }
  }

  // TODO: temporary sort and type filter menus, replace with the real browser UI
  @Composable
  private fun BrowserActions() {
    val preferences = App.graph.fileManagerPreferences
    val sortField by preferences.sortField.collectAsState()
    val sortAscending by preferences.sortAscending.collectAsState()
    val typeNames by preferences.mediaTypes.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }
    Box {
      IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.SortByAlpha, null) }
      DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
        SortField.entries.forEach { field ->
          DropdownMenuItem(
            text = { Text(field.name) },
            trailingIcon = {
              if (field == sortField) {
                Icon(if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null)
              }
            },
            onClick = {
              if (field == sortField) {
                preferences.sortAscending.set(!sortAscending)
              } else {
                preferences.sortField.set(field)
              }
            },
          )
        }
      }
    }
    Box {
      IconButton(onClick = { filterMenu = true }) { Icon(Icons.Default.PermMedia, null) }
      DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
        MediaType.entries.forEach { type ->
          DropdownMenuItem(
            text = { Text(type.name) },
            leadingIcon = { Checkbox(checked = type.name in typeNames, onCheckedChange = null) },
            onClick = {
              preferences.mediaTypes.set(
                if (type.name in typeNames) typeNames - type.name else typeNames + type.name,
              )
            },
          )
        }
      }
    }
  }

  @Composable
  private fun rowColor(index: Int) = if (index % 2 == 1) {
    MaterialTheme.colorScheme.surfaceContainerLow
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
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
    val size: String? by remember { derivedStateOf { length?.asHumanReadableByteCountBin() } }
    val time: String? by remember {
      derivedStateOf {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          lastModified?.let {
            Instant.ofEpochMilli(lastModified).atZone(ZoneId.systemDefault())
              .format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss"))
          }
        } else {
          null
        }
      }
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
  fun fileIcon(isDirectory: Boolean, fileExtension: String): ImageVector {
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
