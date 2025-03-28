package live.mehiz.mpvkt.ui.utils

import java.io.File

/**
 * Sorts files/directories alphabetically while giving directories priority
 * credit goes to mpv-android
 */
class FilesComparator : Comparator<File> {
  override fun compare(o1: File?, o2: File?): Int {
    val iso1ADirectory = o1!!.isDirectory
    val iso2ADirectory = o2!!.isDirectory
    if (iso1ADirectory != iso2ADirectory) return if (iso2ADirectory) 1 else -1
    return o1.name.compareTo(o2.name, ignoreCase = true)
  }
}
