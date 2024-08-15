package live.mehiz.mpvkt.ui.utils

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile

/**
 * Sorts files/directories alphabetically while giving directories priority
 * credit goes to mpv-android
 */
class FilesComparator(
  private val fileManager: FileManager
) : Comparator<AbstractFile> {
  override fun compare(o1: AbstractFile?, o2: AbstractFile?): Int {
    val iso1ADirectory = fileManager.isDirectory(o1!!)
    val iso2ADirectory = fileManager.isDirectory(o2!!)
    if (iso1ADirectory != iso2ADirectory) return if (iso2ADirectory) 1 else -1
    return fileManager.getName(o1).compareTo(fileManager.getName(o2), ignoreCase = true)
  }
}
