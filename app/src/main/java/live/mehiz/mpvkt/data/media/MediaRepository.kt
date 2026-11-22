package live.mehiz.mpvkt.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import live.mehiz.mpvkt.domain.media.DirStats
import live.mehiz.mpvkt.domain.media.MediaFile
import live.mehiz.mpvkt.domain.media.MediaQuery
import live.mehiz.mpvkt.domain.media.MediaType
import live.mehiz.mpvkt.domain.media.SortField
import live.mehiz.mpvkt.domain.media.subtitleExtensions
import kotlin.time.Duration.Companion.milliseconds

private const val DATA = MediaStore.MediaColumns.DATA
private const val NAME = MediaStore.MediaColumns.DISPLAY_NAME
private const val SIZE = MediaStore.MediaColumns.SIZE
private const val DATE = MediaStore.MediaColumns.DATE_MODIFIED
private const val TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE

private const val DURATION = "duration"

private const val OBSERVER_DEBOUNCE_MS = 300L

private val FILE_PROJECTION = arrayOf(MediaStore.MediaColumns._ID, DATA, NAME, SIZE, DATE, DURATION, TYPE)

class MediaRepository(context: Context) {
  private val resolver: ContentResolver = context.contentResolver
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  private val collection: Uri = filesUri()

  // Uris the observer reported since the last emission, null means "rescan everything"
  private val pendingChanges = mutableSetOf<Uri?>()

  @OptIn(FlowPreview::class)
  private val changedUris: Flow<Set<Uri?>> = merge(
    callbackFlow {
      val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
          synchronized(pendingChanges) { pendingChanges += uri }
          trySend(Unit)
        }
      }
      resolver.registerContentObserver("content://${MediaStore.AUTHORITY}".toUri(), true, observer)
      awaitClose { resolver.unregisterContentObserver(observer) }
    },
    manualRefresh.onEach { synchronized(pendingChanges) { pendingChanges += null } },
  )
    .debounce(OBSERVER_DEBOUNCE_MS.milliseconds)
    .map { synchronized(pendingChanges) { pendingChanges.toSet().also { pendingChanges.clear() } } }
    .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000))

  val changes: Flow<Unit> = changedUris.map { }

  fun refresh() = manualRefresh.tryEmit(Unit)

  fun observeDirectories(types: Set<MediaType>): Flow<Map<String, DirStats>> = flow {
    val files = HashMap<Long, MediaFile>()
    fun rescan() {
      files.clear()
      queryAll(types).associateByTo(files) { it.id }
    }
    rescan()
    emit(directoryStats(files.values))
    changedUris.collect { uris ->
      // notifications for a single row end in its id, anything else could have touched any row
      val ids = uris.map { it?.lastPathSegment?.toLongOrNull() }
      if (null in ids) {
        rescan()
      } else {
        val ids = ids.filterNotNull()
        val found = queryByIds(types, ids).associateBy { it.id }
        ids.forEach { id -> found[id]?.let { files[id] = it } ?: files.remove(id) }
      }
      emit(directoryStats(files.values))
    }
  }
    .distinctUntilChanged()
    .flowOn(Dispatchers.IO)

  fun pagingSource(query: MediaQuery): PagingSource<Int, MediaFile> = MediaPagingSource(query)

  private fun queryAll(types: Set<MediaType>): List<MediaFile> {
    val (selection, args) = selectionFor(MediaQuery(types, SortField.Name, true))
    return orWhenUnreadable(emptyList()) {
      resolver.query(collection, FILE_PROJECTION, selection, args, null)?.use { it.toFiles() }.orEmpty()
    }
  }

  private fun queryByIds(types: Set<MediaType>, ids: List<Long>): List<MediaFile> {
    val (selection, args) = selectionFor(MediaQuery(types, SortField.Name, true))
    val byId = "$selection AND ${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",")})"
    return orWhenUnreadable(emptyList()) {
      resolver.query(collection, FILE_PROJECTION, byId, args, null)?.use { it.toFiles() }.orEmpty()
    }
  }

  private fun directoryStats(files: Collection<MediaFile>): Map<String, DirStats> {
    val stats = HashMap<String, DirStats>()
    files.forEach { file ->
      val directory = file.path.substringBeforeLast('/', "")
      if (directory.isNotEmpty()) {
        val row = DirStats(1, file.size, file.durationMs, file.dateModified)
        stats[directory] = stats[directory]?.plus(row) ?: row
      }
    }
    return stats
  }

  private fun queryFiles(query: MediaQuery, limit: Int, offset: Int): List<MediaFile> {
    val (selection, args) = selectionFor(query)
    val direction = if (query.ascending) "ASC" else "DESC"
    val column = when (query.sort) {
      SortField.Name -> "$NAME COLLATE NOCASE"
      SortField.Date -> DATE
      SortField.Size -> SIZE
      SortField.Length -> DURATION
    }
    val order = "$column $direction, ${MediaStore.MediaColumns._ID} $direction"

    val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bundle = Bundle().apply {
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
        putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
      }
      resolver.query(collection, FILE_PROJECTION, bundle, null)
    } else {
      // before Android 11 the provider ignores the limit arguments but passes this through to SQLite
      resolver.query(collection, FILE_PROJECTION, selection, args, "$order LIMIT $limit OFFSET $offset")
    }
    return cursor?.use { it.toFiles() }.orEmpty()
  }

  private fun Cursor.toFiles(): List<MediaFile> {
    val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
    val path = getColumnIndexOrThrow(DATA)
    val name = getColumnIndexOrThrow(NAME)
    val size = getColumnIndexOrThrow(SIZE)
    val date = getColumnIndexOrThrow(DATE)
    val duration = getColumnIndexOrThrow(DURATION)
    val type = getColumnIndexOrThrow(TYPE)
    val files = ArrayList<MediaFile>(count)
    while (moveToNext()) {
      val filePath = getString(path) ?: continue
      files += MediaFile(
        id = getLong(id),
        path = filePath,
        name = getString(name) ?: filePath.substringAfterLast('/'),
        size = getLong(size),
        dateModified = getLong(date),
        durationMs = getLong(duration),
        type = mediaTypeOf(getInt(type)),
      )
    }
    return files
  }

  private inner class MediaPagingSource(private val query: MediaQuery) : PagingSource<Int, MediaFile>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaFile> = withContext(Dispatchers.IO) {
      val key = params.key ?: 0
      val start = if (params is LoadParams.Prepend) maxOf(0, key - params.loadSize) else key
      val limit = if (params is LoadParams.Prepend) key - start else params.loadSize
      val files = try {
        orWhenVolumeMissing(emptyList()) { queryFiles(query, limit, start) }
      } catch (e: SecurityException) {
        return@withContext LoadResult.Error(e)
      }
      LoadResult.Page(
        data = files,
        prevKey = start.takeIf { it > 0 },
        nextKey = (start + files.size).takeIf { files.size == limit },
      )
    }

    override fun getRefreshKey(state: PagingState<Int, MediaFile>): Int? =
      state.anchorPosition?.let { maxOf(0, it - state.config.initialLoadSize / 2) }
  }
}

internal fun filesUri(): Uri = MediaStore.Files.getContentUri(
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL else "external"
)

internal fun MediaFile.contentUri(): Uri {
  val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL else "external"
  val base = when (type) {
    MediaType.Video -> MediaStore.Video.Media.getContentUri(volume)
    MediaType.Audio -> MediaStore.Audio.Media.getContentUri(volume)
    MediaType.Image -> MediaStore.Images.Media.getContentUri(volume)
    MediaType.Subtitle -> MediaStore.Files.getContentUri(volume)
  }
  return ContentUris.withAppendedId(base, id)
}

private inline fun <T> orWhenVolumeMissing(default: T, block: () -> T): T = try {
  block()
} catch (e: IllegalArgumentException) {
  if (e.message?.startsWith("Volume ") == true) default else throw e
}

private inline fun <T> orWhenUnreadable(default: T, block: () -> T): T = try {
  orWhenVolumeMissing(default, block)
} catch (_: SecurityException) {
  default
}

private fun String.escapeLike() = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

internal fun selectionFor(query: MediaQuery): Pair<String, Array<String>> {
  val clauses = mutableListOf<String>()
  val args = mutableListOf<String>()

  val mediaTypes = query.types.mapNotNull {
    when (it) {
      MediaType.Video -> MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
      MediaType.Audio -> MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
      MediaType.Image -> MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
      MediaType.Subtitle -> null
    }
  }
  val typeClauses = mutableListOf<String>()
  if (mediaTypes.isNotEmpty()) typeClauses += "$TYPE IN (${mediaTypes.joinToString(",")})"
  if (MediaType.Subtitle in query.types) subtitleExtensions.mapTo(typeClauses) { "$DATA LIKE '%.$it'" }
  clauses += if (typeClauses.isEmpty()) "0" else typeClauses.joinToString(" OR ", "(", ")")
  clauses += "$DATA IS NOT NULL"

  // LIKE ignores case for ascii, using substr instead
  query.directory?.let { directory ->
    val prefix = directory.trimEnd('/') + "/"
    // SQLite counts characters, not UTF-16 units, love sql
    val length = prefix.codePointCount(0, prefix.length)
    clauses += "substr($DATA, 1, ?) = ?"
    args += length.toString()
    args += prefix
    if (!query.recursive) {
      clauses += "instr(substr($DATA, ?), '/') = 0"
      args += (length + 1).toString()
    }
  }
  if (query.search.isNotBlank()) {
    clauses += "$NAME LIKE ? ESCAPE '\\'"
    args += "%${query.search.trim().escapeLike()}%"
  }
  return clauses.joinToString(" AND ") to args.toTypedArray()
}

private fun mediaTypeOf(column: Int): MediaType = when (column) {
  MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaType.Video
  MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaType.Audio
  MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaType.Image
  else -> MediaType.Subtitle
}
