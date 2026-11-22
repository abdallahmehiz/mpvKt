package live.mehiz.mpvkt.domain.media

import androidx.compose.runtime.Immutable

enum class MediaType { Video, Audio, Image, Subtitle }
enum class SortField { Name, Date, Size, Length }

@Immutable
data class MediaFile(
  val id: Long,
  val path: String,
  val name: String,
  val size: Long,
  val dateModified: Long,
  val durationMs: Long,
  val type: MediaType,
)

@Immutable
data class MediaDirectory(
  val path: String,
  val name: String,
  val stats: DirStats,
)

data class DirStats(
  val count: Int = 0,
  val size: Long = 0,
  val durationMs: Long = 0,
  val dateModified: Long = 0,
) {
  operator fun plus(other: DirStats) = DirStats(
    count + other.count,
    size + other.size,
    durationMs + other.durationMs,
    maxOf(dateModified, other.dateModified),
  )
}

data class MediaQuery(
  val types: Set<MediaType>,
  val sort: SortField,
  val ascending: Boolean,
  val directory: String? = null,
  val recursive: Boolean = true,
  val search: String = "",
)

val subtitleExtensions = listOf("srt", "ass", "ssa", "vtt", "sub", "sup", "idx", "pgs", "smi", "lrc", "ttml")

private val volumeRoot = Regex("^/storage/(emulated/\\d+|[^/]+)")

fun volumeRootOf(path: String): String = volumeRoot.find(path)?.value ?: "/"

fun folderName(path: String): String = path.substringAfterLast('/').ifEmpty { "/" }

fun childFolders(stats: Map<String, DirStats>, parent: String): List<MediaDirectory> {
  val prefix = parent.trimEnd('/') + "/"
  val children = LinkedHashMap<String, DirStats>()
  for ((dir, s) in stats) {
    if (!dir.startsWith(prefix)) continue
    val child = prefix + dir.substring(prefix.length).substringBefore('/')
    children[child] = (children[child] ?: DirStats()) + s
  }
  return children.map { (path, s) -> MediaDirectory(path, folderName(path), s) }
}

fun volumeFolders(stats: Map<String, DirStats>): List<MediaDirectory> =
  stats.entries.groupBy({ volumeRootOf(it.key) }, { it.value }).map { (root, list) ->
    MediaDirectory(root, folderName(root), list.reduce(DirStats::plus))
  }

fun List<MediaDirectory>.sortedBy(field: SortField, ascending: Boolean): List<MediaDirectory> {
  val comparator: Comparator<MediaDirectory> = when (field) {
    SortField.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    SortField.Date -> compareBy { it.stats.dateModified }
    SortField.Size -> compareBy { it.stats.size }
    SortField.Length -> compareBy { it.stats.durationMs }
  }
  return sortedWith(if (ascending) comparator else comparator.reversed())
}
